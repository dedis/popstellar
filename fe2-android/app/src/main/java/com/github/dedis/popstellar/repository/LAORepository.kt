package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.view.LaoView
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.database.lao.LAOEntity
import com.github.dedis.popstellar.utility.GeneralUtils.buildLifecycleCallback
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class LAORepository @Inject constructor(appDatabase: AppDatabase, application: Application) {
  private val laoDao: LAODao

  /** Thread-safe map used to store the laos by their unique identifiers */
  private val laoById = ConcurrentHashMap<String, Lao>()

  /** Thread-safe map that maps a lao identifier to an observable over its lao view */
  private val subjectById = ConcurrentHashMap<String, Subject<LaoView>>()

  /** Observable over the list of laos' identifiers */
  private val laosSubject = BehaviorSubject.create<List<String>>()
  private val disposables = CompositeDisposable()

  init {
    laoDao = appDatabase.laoDao()

    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(buildLifecycleCallback(consumerMap))
    loadPersistentStorage()
  }

  /**
   * This functions is called on start to load all the laos in memory as they must be displayed in
   * the home list. Given the fact that we load every lao in memory at the beginning a cache is not
   * necessary. This is also possible memory-wise as usually the number of laos is very limited.
   * This call is asynchronous so it's performed in background not blocking the main thread.
   */
  private fun loadPersistentStorage() {
    disposables.add(
        laoDao.allLaos
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { laos: List<Lao>? ->
                  if (laos.isNullOrEmpty()) {
                    Timber.tag(TAG).d("No LAO has been found in the database")
                    return@subscribe
                  }
                  laos.forEach(
                      Consumer { lao: Lao ->
                        laoById[lao.id] = lao
                        subjectById[lao.id] = BehaviorSubject.createDefault(LaoView(lao))
                      })
                  val laoIds = laos.stream().map { obj: Lao -> obj.id }.collect(Collectors.toList())
                  laosSubject.toSerialized().onNext(laoIds)
                  Timber.tag(TAG).d("Loaded all the LAOs from database: %s", laoIds)
                },
                { err: Throwable ->
                  Timber.tag(TAG).e(err, "Error loading the LAOs from the database")
                }))
  }

  /**
   * Retrieves the Lao in a given channel
   *
   * @param channel the channel on which the Lao was created
   * @return the Lao corresponding to this channel
   */
  @Throws(UnknownLaoException::class)
  fun getLaoByChannel(channel: Channel): Lao {
    Timber.tag(TAG).d("querying lao for channel %s", channel)
    return laoById[channel.extractLaoId()] ?: throw UnknownLaoException(channel.extractLaoId())
  }

  /**
   * Checks that a LAO with the given id exists in the repo
   *
   * @param laoId the LAO id to check
   * @return true if a LAO with the given id exists
   */
  fun containsLao(laoId: String): Boolean {
    return laoById.containsKey(laoId)
  }

  val allLaoIds: Observable<List<String>>
    get() = laosSubject

  fun getLaoObservable(laoId: String): Observable<LaoView> {
    subjectById.computeIfAbsent(laoId) { BehaviorSubject.create() }
    return subjectById.getValue(laoId)
  }

  @Throws(UnknownLaoException::class)
  fun getLaoView(id: String): LaoView {
    val lao = laoById[id]
    if (lao == null) {
      // In some Android devices after putting the application in the
      // background or locking the screen it happens that the lao is not found.
      // This could be due to the ram being cleared, so a I/O check could help avoiding this
      // scenario.
      val laoFromDb = laoDao.getLaoById(id)
      if (laoFromDb == null) {
        throw UnknownLaoException(id)
      } else {
        // Restore the lao in memory
        updateLao(laoFromDb)
      }
    }
    return LaoView(lao)
  }

  @Throws(UnknownLaoException::class)
  fun getLaoViewByChannel(channel: Channel): LaoView {
    return getLaoView(channel.extractLaoId())
  }

  fun updateLao(lao: Lao) {
    Timber.tag(TAG).d("Updating Lao %s", lao)
    val laoView = LaoView(lao)
    val laoEntity = LAOEntity(lao.id, lao)

    // Update the persistent storage in background (replace if already existing)
    disposables.add(
        laoDao
            .insert(laoEntity)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Timber.tag(TAG).d("Persisted Lao %s", lao) },
                { err: Throwable -> Timber.tag(TAG).e(err, "Error persisting Lao %s", lao) }))

    if (laoById.containsKey(lao.id)) {
      // If the lao already exists, we can push the next update
      laoById[lao.id] = lao
      // Update observer if present
      val subject = subjectById[lao.id]
      subject?.toSerialized()?.onNext(laoView)
    } else {
      // Otherwise, create the entry
      laoById[lao.id] = lao
      // Update lao list
      laosSubject.toSerialized().onNext(ArrayList(laoById.keys))
      subjectById[lao.id] = BehaviorSubject.createDefault(laoView)
    }
  }

  /** This function clears the repository */
  fun clearRepository() {
    Timber.tag(TAG).d("Clearing LAORepository...")
    laoById.clear()
    subjectById.clear()
    laosSubject.toSerialized().onNext(ArrayList())
  }

  fun addDisposable(disposable: Disposable) {
    disposables.add(disposable)
  }

  companion object {
    private val TAG = LAORepository::class.java.simpleName
  }
}
