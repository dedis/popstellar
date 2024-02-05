package com.github.dedis.popstellar.repository

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.Meeting.Companion.generateCreateMeetingId
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.digitalcash.HashDao
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionDao
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity
import com.github.dedis.popstellar.repository.database.witnessing.WitnessDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.UnknownElectionException
import com.github.dedis.popstellar.utility.error.UnknownMeetingException
import com.github.dedis.popstellar.utility.error.UnknownRollCallException
import io.reactivex.Completable
import io.reactivex.Single
import java.time.Instant
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule

@RunWith(AndroidJUnit4::class)
class WitnessingRepositoryTest {
  @Mock private lateinit var appDatabase: AppDatabase
  @Mock private lateinit var rollCallDao: RollCallDao
  @Mock private lateinit var witnessDao: WitnessDao
  @Mock private lateinit var witnessingDao: WitnessingDao
  @Mock private lateinit var pendingDao: PendingDao
  @Mock private lateinit var transactionDao: TransactionDao
  @Mock private lateinit var hashDao: HashDao
  @Mock private lateinit var electionDao: ElectionDao
  @Mock private lateinit var meetingDao: MeetingDao

  private lateinit var witnessingRepository: WitnessingRepository
  private lateinit var rollCallRepo: RollCallRepository
  private lateinit var electionRepo: ElectionRepository
  private lateinit var meetingRepo: MeetingRepository

  @JvmField @Rule(order = 0) val mockitoRule: MockitoRule = MockitoJUnit.rule()

  @Before
  fun setUp() {
    val application = ApplicationProvider.getApplicationContext<Application>()

    Mockito.`when`(appDatabase.witnessDao()).thenReturn(witnessDao)
    Mockito.`when`(appDatabase.witnessingDao()).thenReturn(witnessingDao)
    Mockito.`when`(appDatabase.pendingDao()).thenReturn(pendingDao)
    Mockito.`when`(appDatabase.rollCallDao()).thenReturn(rollCallDao)
    Mockito.`when`(appDatabase.electionDao()).thenReturn(electionDao)
    Mockito.`when`(appDatabase.meetingDao()).thenReturn(meetingDao)
    Mockito.`when`(appDatabase.transactionDao()).thenReturn(transactionDao)
    Mockito.`when`(appDatabase.hashDao()).thenReturn(hashDao)

    rollCallRepo = RollCallRepository(appDatabase, application)
    electionRepo = ElectionRepository(appDatabase, application)
    meetingRepo = MeetingRepository(appDatabase, application)
    val digitalCashRepo = DigitalCashRepository(appDatabase, application)
    witnessingRepository =
      WitnessingRepository(
        appDatabase,
        application,
        rollCallRepo,
        electionRepo,
        meetingRepo,
        digitalCashRepo
      )

    Mockito.`when`(witnessDao.insertAll(ArgumentMatchers.anyList()))
      .thenReturn(Completable.complete())
    Mockito.`when`(witnessDao.getWitnessesByLao(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(witnessDao.isWitness(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any()))
      .thenReturn(0)

    Mockito.`when`(witnessingDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
    Mockito.`when`(witnessingDao.getWitnessMessagesByLao(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(
        witnessingDao.deleteMessagesByIds(ArgumentMatchers.anyString(), ArgumentMatchers.anySet())
      )
      .thenReturn(Completable.complete())

    Mockito.`when`(pendingDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(pendingDao.getPendingObjectsFromLao(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(pendingDao.removePendingObject(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())

    Mockito.`when`(rollCallDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())
    Mockito.`when`(rollCallDao.getRollCallsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))

    Mockito.`when`(electionDao.getElectionsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(electionDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())

    Mockito.`when`(meetingDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())
    Mockito.`when`(meetingDao.getMeetingsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))

    Mockito.`when`(hashDao.getDictionaryByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(hashDao.deleteByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())
    Mockito.`when`(hashDao.insertAll(ArgumentMatchers.anyList())).thenReturn(Completable.complete())

    Mockito.`when`(transactionDao.getTransactionsByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Single.just(emptyList()))
    Mockito.`when`(transactionDao.deleteByLaoId(ArgumentMatchers.anyString()))
      .thenReturn(Completable.complete())
    Mockito.`when`(transactionDao.insert(MockitoKotlinHelpers.any()))
      .thenReturn(Completable.complete())

    witnessingRepository.addWitnesses(LAO_ID, WITNESSES)
    witnessingRepository.addWitnessMessage(LAO_ID, WITNESS_MESSAGE)
  }

  @After
  fun tearDown() {
    appDatabase.close()
  }

  @Test
  @Throws(UnknownRollCallException::class)
  fun achieveSignatureThresholdPerformActionRollCall() {
    val pendingEntity = PendingEntity(MESSAGE_ID, LAO_ID, ROLL_CALL)
    witnessingRepository.addPendingEntity(pendingEntity)
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS)

    // Verify the roll call has been added to the repo
    Assert.assertEquals(ROLL_CALL, rollCallRepo.getRollCallWithId(LAO_ID, ROLL_CALL.id))
  }

  @Test
  @Throws(UnknownElectionException::class)
  fun achieveSignatureThresholdPerformActionElection() {
    val pendingEntity = PendingEntity(MESSAGE_ID, LAO_ID, ELECTION)
    witnessingRepository.addPendingEntity(pendingEntity)
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS)

    // Verify the election has been added to the repo
    Assert.assertEquals(ELECTION, electionRepo.getElection(LAO_ID, ELECTION.id))
  }

  @Test
  @Throws(UnknownMeetingException::class)
  fun achieveSignatureThresholdPerformActionMeeting() {
    val pendingEntity = PendingEntity(MESSAGE_ID, LAO_ID, MEETING)
    witnessingRepository.addPendingEntity(pendingEntity)
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS)

    // Verify the meeting has been added to the repo
    Assert.assertEquals(MEETING, meetingRepo.getMeetingWithId(LAO_ID, MEETING.id))
  }

  companion object {
    private const val LAO_ID = "LAO_ID"
    private val MESSAGE_ID = Base64DataUtils.generateMessageID()
    private val WITNESS = Base64DataUtils.generateKeyPair().publicKey
    private val WITNESSES: Set<PublicKey> = HashSet(listOf(WITNESS))
    private val WITNESS_MESSAGE = WitnessMessage(MESSAGE_ID)
    private val CREATION = Instant.now().epochSecond
    private val ROLL_CALL =
      RollCall(
        LAO_ID,
        LAO_ID,
        "title",
        CREATION,
        CREATION + 10,
        CREATION + 20,
        EventState.CREATED,
        HashSet(),
        "loc",
        ""
      )
    private val ELECTION =
      ElectionBuilder(LAO_ID, 100321014, "Election")
        .setElectionVersion(ElectionVersion.OPEN_BALLOT)
        .build()
    private val MEETING =
      Meeting(
        generateCreateMeetingId(LAO_ID, CREATION, "name"),
        "name",
        CREATION,
        CREATION,
        CREATION,
        "",
        CREATION,
        "",
        ArrayList()
      )
  }
}
