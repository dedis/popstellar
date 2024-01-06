package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.OutputObject
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.digitalcash.HashDao
import com.github.dedis.popstellar.repository.database.digitalcash.HashEntity
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionDao
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionEntity
import com.github.dedis.popstellar.utility.ActivityUtils.buildLifecycleCallback
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.Consumer
import java.util.stream.Collectors
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class DigitalCashRepository
@Inject
constructor(appDatabase: AppDatabase, application: Application) {
  private val transactionsByLao: MutableMap<String, LaoTransactions> = HashMap()
  private val transactionDao: TransactionDao
  private val hashDao: HashDao
  private val disposables = CompositeDisposable()

  init {
    transactionDao = appDatabase.transactionDao()
    hashDao = appDatabase.hashDao()
    val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> =
        EnumMap(Lifecycle.Event::class.java)
    consumerMap[Lifecycle.Event.ON_STOP] = Consumer { disposables.clear() }
    application.registerActivityLifecycleCallbacks(buildLifecycleCallback(consumerMap))
  }

  /**
   * Get the list of transactions that a given user has performed.
   *
   * @param laoId of the lao the transactions are part of
   * @param user public key
   * @return a list of transaction objects
   */
  fun getTransactions(laoId: String, user: PublicKey): List<TransactionObject>? {
    return getLaoTransactions(laoId).getTransactions(user)
  }

  /**
   * This returns an observable on the list of transactions that a given user performs.
   *
   * @param laoId of the lao the transactions are part of
   * @param user public key
   * @return an observable that is notified whenever a new transaction is added/updated
   */
  fun getTransactionsObservable(
      laoId: String,
      user: PublicKey
  ): Observable<List<TransactionObject>> {
    return getLaoTransactions(laoId).getTransactionsObservable(user)
  }

  /**
   * The function returns the user balance in miniLAOs based on the transactions he's involved into.
   *
   * @param laoId of the lao the transactions are part of
   * @param user public key
   * @return miniLAOs amount as long
   */
  fun getUserBalance(laoId: String, user: PublicKey): Long {
    return getLaoTransactions(laoId).getUserBalance(user)
  }

  /**
   * This updates and persist a transaction in a digital cash state of a lao.
   *
   * @param laoId of the lao the transaction is part of
   * @param transaction digital cash transaction to add
   * @throws NoRollCallException if no roll call attendees is found
   */
  @Throws(NoRollCallException::class)
  fun updateTransactions(laoId: String, transaction: TransactionObject) {
    Timber.tag(TAG).d("updating transactions on Lao %s and transaction %s", laoId, transaction)
    getLaoTransactions(laoId).updateTransactions(transaction, true)
  }

  /**
   * This function initializes the digital cash for a given lao. This happens after a rollcall is
   * closed to reset the previous state and add the current attendees.
   *
   * @param laoId of the lao whose digital cash we are looking for
   * @param attendees set of public keys of the attendees the the last closed roll call
   */
  fun initializeDigitalCash(laoId: String, attendees: List<PublicKey>) {
    getLaoTransactions(laoId).initializeDigitalCash(attendees)
  }

  @Synchronized
  private fun getLaoTransactions(laoId: String): LaoTransactions {
    // Create the lao transactions object if it is not present yet
    return transactionsByLao.computeIfAbsent(laoId) { LaoTransactions(laoId, this) }
  }

  private class LaoTransactions(
      private val laoId: String,
      private val repository: DigitalCashRepository
  ) {
    /**
     * Thread-safe map that maps the users' public keys to a thread-safe list of transactions which
     * they have been involved into (either senders or receivers)
     */
    private val transactions =
        ConcurrentHashMap<PublicKey, ConcurrentLinkedQueue<TransactionObject>>()

    /** Thread-safe map that maps the users' public keys to the observable on their transactions */
    private val transactionsSubject =
        ConcurrentHashMap<PublicKey, Subject<List<TransactionObject>>>()

    /** Thread-safe dictionary to maps the hash to the user's public key */
    private val hashDictionary = ConcurrentHashMap<String, PublicKey>()

    init {
      loadLaoState()
    }

    /**
     * This resets the digital cash state. This function is called each time a rc is closed.
     *
     * @param attendees the attendees of the closed roll call
     */
    fun initializeDigitalCash(attendees: List<PublicKey>) {
      Timber.tag(TAG).d("initializing digital cash with attendees %s", attendees)

      // Clear the transactions on the database for the given lao
      repository.disposables.add(
          repository.transactionDao
              .deleteByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({
                Timber.tag(TAG).d("Cleared the transactions in the db for lao %s", laoId)
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "Error in clearing transactions for lao %s", laoId)
              })

      // Clear the memory
      hashDictionary.clear()
      transactions.clear()
      transactionsSubject.clear()
      transactionsSubject.values.forEach(
          Consumer { observer: Subject<List<TransactionObject>> ->
            observer.toSerialized().onComplete()
          })

      // Reset the hash dictionary
      val hashEntities: MutableList<HashEntity> = ArrayList()
      attendees.forEach(
          Consumer { publicKey: PublicKey ->
            val hash = publicKey.computeHash()
            hashDictionary[hash] = publicKey
            // Save the mapping in a list
            hashEntities.add(HashEntity(hash, laoId, publicKey))
          })

      // Save all the entries at once to minimize I/O accesses
      // Ensure to delete before adding the new entries
      repository.disposables.add(
          repository.hashDao
              .deleteByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({
                Timber.tag(TAG).d("Cleared the hash dictionary in the db for lao %s", laoId)
                // After having it deleted, save the new entities
                repository.disposables.add(
                    repository.hashDao
                        .insertAll(hashEntities)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({
                          Timber.tag(TAG)
                              .d("Successfully persisted hash dictionary for lao %s", laoId)
                        }) { err: Throwable ->
                          Timber.tag(TAG)
                              .e(err, "Error in persisting the hash dictionary for lao %s", laoId)
                        })
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "Error in clearing the hash dictionary for lao %s", laoId)
              })
    }

    @Throws(NoRollCallException::class)
    fun updateTransactions(transaction: TransactionObject, toBeStored: Boolean) {
      if (hashDictionary.isEmpty()) {
        throw NoRollCallException("No roll call attendees could be found")
      }
      for (current in getReceiversTransaction(transaction)) {
        var transactionList = transactions[current]
        if (transactionList == null) {
          transactionList = ConcurrentLinkedQueue()
          transactionList.add(transaction)

          // An empty subject might have been created already
          if (transactionsSubject.containsKey(current)) {
            // Updating subject
            transactionsSubject[current]?.toSerialized()?.onNext(ArrayList(transactionList))
          } else {
            // Creating new subject
            transactionsSubject[current] = BehaviorSubject.createDefault(ArrayList(transactionList))
          }

          // Add it to the map
          transactions[current] = transactionList
        } else if (!transactionList.contains(transaction)) {
          transactionList.add(transaction)
          transactionsSubject[current]?.toSerialized()?.onNext(ArrayList(transactionList))
        }
      }

      // Store the transaction in the db if the flag is true
      if (toBeStored) {
        val transactionEntity = TransactionEntity(laoId, transaction)
        repository.disposables.add(
            repository.transactionDao
                .insert(transactionEntity)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                  Timber.tag(TAG)
                      .d("Successfully persisted transaction %s", transaction.transactionId)
                }) { err: Throwable ->
                  Timber.tag(TAG)
                      .e(err, "Error in persisting the transaction %s", transaction.transactionId)
                })
      }
    }

    fun getTransactionsObservable(user: PublicKey): Observable<List<TransactionObject>> {
      return transactionsSubject.computeIfAbsent(user) {
        BehaviorSubject.createDefault(ArrayList())
      }
    }

    fun getTransactions(user: PublicKey): List<TransactionObject>? {
      val transactionList = transactions[user]
      return if (transactionList == null) null else ArrayList(transactionList)
    }

    private fun getReceiversTransaction(transaction: TransactionObject): List<PublicKey> {
      // For each output, we get the hash of the public key. Then for each hash we return the
      // preimage (the public key)
      return transaction.outputs
          .stream()
          .map { obj: OutputObject -> obj.pubKeyHash }
          .map { hash: String ->
            val key =
                hashDictionary[hash]
                    ?: throw IllegalStateException("The hash is not in dictionary of known hashes")
            key
          }
          .collect(Collectors.toList())
    }

    fun getUserBalance(user: PublicKey?): Long {
      val transactionList = transactions[user]
      return transactionList
          ?.stream()
          ?.mapToLong { transaction: TransactionObject -> transaction.getSumForUser(user) }
          ?.sum() ?: 0
    }

    /**
     * This function loads the state of the digital cash for a given lao, retrieving the list of
     * transactions and the hash dictionary. This is done only once per LAO and when the user
     * accesses such LAO's digital cash section.
     */
    private fun loadLaoState() {
      repository.disposables.add(
          repository.hashDao
              .getDictionaryByLaoId(laoId)
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe({ hashEntities: List<HashEntity> ->
                // Firstly load the dictionary
                hashEntities.forEach(
                    Consumer { hashEntity: HashEntity ->
                      hashDictionary[hashEntity.hash] = hashEntity.publicKey
                    })
                Timber.tag(TAG).d("Retrieved the hash dictionary from db")
                // Then load the transactions
                repository.disposables.add(
                    repository.transactionDao
                        .getTransactionsByLaoId(laoId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe({ transactionObjects: List<TransactionObject> ->
                          transactionObjects.forEach(
                              Consumer { transactionObject: TransactionObject ->
                                Timber.tag(TAG)
                                    .d(
                                        "Retrieved transaction %s from db",
                                        transactionObject.transactionId)
                                try {
                                  updateTransactions(transactionObject, false)
                                } catch (e: NoRollCallException) {
                                  Timber.tag(TAG)
                                      .e(e, "No roll call exception to load for lao %s", laoId)
                                  // This exception can't ever be thrown, as if the
                                  // transactions are in the db,
                                  // then they have a valid public key associated
                                }
                              })
                        }) { err: Throwable ->
                          Timber.tag(TAG).e(err, "No transaction to load for lao %s", laoId)
                        })
              }) { err: Throwable ->
                Timber.tag(TAG).e(err, "No hash dictionary to load for lao %s", laoId)
              })
    }
  }

  companion object {
    val TAG: String = DigitalCashRepository::class.java.simpleName
  }
}
