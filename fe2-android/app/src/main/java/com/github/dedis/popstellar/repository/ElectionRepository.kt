package com.github.dedis.popstellar.repository

import android.app.Activity
import android.app.Application
import androidx.lifecycle.Lifecycle
import com.github.dedis.popstellar.model.objects.Channel
import com.github.dedis.popstellar.model.objects.Election
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao
import com.github.dedis.popstellar.repository.database.event.election.ElectionEntity
import com.github.dedis.popstellar.utility.ActivityUtils.buildLifecycleCallback
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import timber.log.Timber
import java.util.Collections
import java.util.EnumMap
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * This class is the repository of the elections events
 *
 *
 * Its main purpose is to store elections and publish updates
 */
@Singleton
class ElectionRepository @Inject constructor(appDatabase: AppDatabase, application: Application) {
    private val electionsByLao: MutableMap<String, LaoElections> = HashMap()
    private val electionDao: ElectionDao
    private val disposables = CompositeDisposable()

    init {
        electionDao = appDatabase.electionDao()
        val consumerMap: MutableMap<Lifecycle.Event, Consumer<Activity>> = EnumMap(
            Lifecycle.Event::class.java
        )
        consumerMap[Lifecycle.Event.ON_STOP] =
            Consumer { disposables.clear() }
        application.registerActivityLifecycleCallbacks(
            buildLifecycleCallback(consumerMap)
        )
    }

    /**
     * Update the election state.
     *
     *
     * It can be a new election or an update.
     *
     * @param election the election to update
     */
    fun updateElection(election: Election) {
        // Persist the election
        disposables.add(
            electionDao
                .insert(ElectionEntity(election))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { Timber.tag(TAG).d("Successfully persisted election %s", election.id) }
                ) { err: Throwable ->
                    Timber.tag(TAG).e(err, "Error in persisting election %s", election.id)
                })

        // Get the lao state and update the election
        getLaoElections(election.channel.extractLaoId()).updateElection(election)
    }

    /**
     * Retrieve an election state given its Lao and its ID
     *
     * @param laoId of the lao the election is part of
     * @param electionId id of the election
     * @return the election state
     * @throws UnknownElectionException if no election with this id exist in this lao
     */
    @Throws(UnknownElectionException::class)
    fun getElection(laoId: String, electionId: String): Election {
        return getLaoElections(laoId).getElection(electionId)
    }

    /**
     * Retrieve an election state given its channel
     *
     * @param channel of the election
     * @return the election state
     * @throws UnknownElectionException if no election with this id exist in the lao
     */
    @Throws(UnknownElectionException::class)
    fun getElectionByChannel(channel: Channel): Election {
        return getElection(channel.extractLaoId(), channel.extractElectionId())
    }

    /**
     * Retrieve an election observable given its Lao and its ID
     *
     *
     * The observable will carry any update made fow this election
     *
     * @param laoId of the lao the election is part of
     * @param electionId id of the election
     * @return the election observable
     * @throws UnknownElectionException if no election with this id exist in this lao
     */
    @Throws(UnknownElectionException::class)
    fun getElectionObservable(
        laoId: String, electionId: String
    ): Observable<Election> {
        return getLaoElections(laoId).getElectionSubject(electionId)
    }

    /**
     * Compute an Observable containing the set of ids of all election in the given Lao
     *
     * @param laoId lao of the elections
     * @return an observable that will be updated with the set of all election's ids
     */
    fun getElectionsObservableInLao(laoId: String): Observable<Set<Election>> {
        return getLaoElections(laoId).getElectionsSubject()
    }

    @Synchronized
    private fun getLaoElections(laoId: String): LaoElections {
        // Create the lao elections object if it is not present yet
        return electionsByLao.computeIfAbsent(laoId) { LaoElections(this, laoId) }
    }

    private class LaoElections(
        private val repository: ElectionRepository,
        private val laoId: String
    ) {
        /** Thread-safe map that stores the elections by their identifiers  */
        private val electionById = ConcurrentHashMap<String, Election>()

        /** Thread-safe map that maps an election id to an observable of it  */
        private val electionSubjects = ConcurrentHashMap<String, Subject<Election>>()

        /** Observable of all the election set  */
        private val electionsSubject = BehaviorSubject.createDefault(emptySet<Election>())

        init {
            loadStorage()
        }

        fun updateElection(election: Election) {
            val id = election.id
            electionById[id] = election
            electionSubjects.putIfAbsent(id, BehaviorSubject.create())
            electionSubjects[id]?.toSerialized()?.onNext(election)
            electionsSubject
                .toSerialized()
                .onNext(Collections.unmodifiableSet(HashSet(electionById.values)))
        }

        @Throws(UnknownElectionException::class)
        fun getElection(electionId: String): Election {
            val election = electionById[electionId]
            return election ?: throw UnknownElectionException(electionId)
        }

        fun getElectionsSubject(): Observable<Set<Election>> {
            return electionsSubject
        }

        @Throws(UnknownElectionException::class)
        fun getElectionSubject(electionId: String): Observable<Election> {
            val electionObservable: Observable<Election>? = electionSubjects[electionId]
            return electionObservable ?: throw UnknownElectionException(electionId)
        }

        /**
         * Load in memory the elections from the disk only when the user clicks on the respective LAO,
         * just needed one time only.
         */
        private fun loadStorage() {
            repository.disposables.add(
                repository.electionDao
                    .getElectionsByLaoId(laoId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { elections: List<Election> ->
                            elections.forEach(
                                Consumer { election: Election ->
                                    updateElection(election)
                                    Timber.tag(TAG).d("Retrieved from db election %s", election.id)
                                })
                        }
                    ) { err: Throwable ->
                        Timber.tag(TAG)
                            .e(err, "No election found in the storage for lao %s", laoId)
                    })
        }
    }

    companion object {
        private val TAG = ElectionRepository::class.java.simpleName
    }
}