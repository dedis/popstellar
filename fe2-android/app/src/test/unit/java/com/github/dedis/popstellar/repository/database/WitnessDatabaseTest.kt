package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.network.method.message.data.Objects
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion
import com.github.dedis.popstellar.model.objects.Election.ElectionBuilder
import com.github.dedis.popstellar.model.objects.Lao.Companion.generateLaoId
import com.github.dedis.popstellar.model.objects.Meeting
import com.github.dedis.popstellar.model.objects.Meeting.Companion.generateCreateMeetingId
import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.WitnessMessage
import com.github.dedis.popstellar.model.objects.event.EventState
import com.github.dedis.popstellar.model.objects.security.MessageID
import com.github.dedis.popstellar.model.objects.security.PublicKey
import com.github.dedis.popstellar.repository.database.witnessing.PendingDao
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity
import com.github.dedis.popstellar.repository.database.witnessing.WitnessDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessEntity
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingDao
import com.github.dedis.popstellar.repository.database.witnessing.WitnessingEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import io.reactivex.observers.TestObserver
import java.time.Instant
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WitnessDatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var witnessDao: WitnessDao
  private lateinit var witnessingDao: WitnessingDao
  private lateinit var pendingDao: PendingDao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    witnessDao = appDatabase.witnessDao()
    witnessingDao = appDatabase.witnessingDao()
    pendingDao = appDatabase.pendingDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertWitnessTest() {
    val witnesses: MutableList<WitnessEntity> = ArrayList()
    witnesses.add(WITNESS_ENTITY)
    val testObserver = witnessDao.insertAll(witnesses).test()

    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun witnessTest() {
    val witnesses: MutableList<WitnessEntity> = ArrayList()
    witnesses.add(WITNESS_ENTITY)
    val testObserver = witnessDao.insertAll(witnesses).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2 =
      witnessDao.getWitnessesByLao(LAO_ID).test().assertValue { witnessList: List<PublicKey> ->
        witnessList.size == 1 && witnessList[0] == WITNESS
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  @Test
  fun isWitnessTest() {
    Assert.assertEquals(0, witnessDao.isWitness(LAO_ID, WITNESS).toLong())
    val witnesses: MutableList<WitnessEntity> = ArrayList()
    witnesses.add(WITNESS_ENTITY)
    val testObserver = witnessDao.insertAll(witnesses).test()

    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
    Assert.assertEquals(1, witnessDao.isWitness(LAO_ID, WITNESS).toLong())
  }

  @Test
  fun insertWitnessingTest() {
    val testObserver = witnessingDao.insert(WITNESSING_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun witnessingTest() {
    val testObserver = witnessingDao.insert(WITNESSING_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2: TestObserver<List<WitnessMessage>?> =
      witnessingDao.getWitnessMessagesByLao(LAO_ID).test().assertValue {
        witnessMessages: List<WitnessMessage> ->
        witnessMessages.size == 1 && witnessMessages[0] == WITNESS_MESSAGE
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  @Test
  fun deleteWitnessingTest() {
    val testObserver = witnessingDao.insert(WITNESSING_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2 = witnessingDao.getWitnessMessagesByLao(LAO_ID).test()
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()

    val filteredIds: MutableSet<MessageID> = HashSet()
    filteredIds.add(MESSAGE_ID)
    val testObserver3 = witnessingDao.deleteMessagesByIds(LAO_ID, filteredIds).test()
    testObserver3.awaitTerminalEvent()
    testObserver3.assertComplete()

    val testObserver4: TestObserver<List<WitnessMessage>?> =
      witnessingDao.getWitnessMessagesByLao(LAO_ID).test().assertValue { obj: List<WitnessMessage?>
        ->
        obj.isEmpty()
      }
    testObserver4.awaitTerminalEvent()
    testObserver4.assertComplete()
  }

  @Test
  fun insertPendingTest() {
    val testObserver = pendingDao.insert(ROLL_CALL_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
  }

  @Test
  fun pendingTest() {
    val testObserverV1 = pendingDao.insert(ROLL_CALL_ENTITY).test()
    val testObserverV2 = pendingDao.insert(ELECTION_ENTITY).test()
    val testObserverV3 = pendingDao.insert(MEETING_ENTITY).test()

    testObserverV1.awaitTerminalEvent()
    testObserverV1.assertComplete()
    testObserverV2.awaitTerminalEvent()
    testObserverV2.assertComplete()
    testObserverV3.awaitTerminalEvent()
    testObserverV3.assertComplete()

    Assert.assertEquals(Objects.ROLL_CALL, ROLL_CALL_ENTITY.objectType)
    Assert.assertEquals(Objects.ELECTION, ELECTION_ENTITY.objectType)
    Assert.assertEquals(Objects.MEETING, MEETING_ENTITY.objectType)

    val testObserver2: TestObserver<List<PendingEntity>?> =
      pendingDao.getPendingObjectsFromLao(LAO_ID).test().assertValue {
        pendingEntities: List<PendingEntity> ->
        pendingEntities.size == 3 &&
          pendingEntities[0].messageID == MESSAGE_ID &&
          pendingEntities[0].laoId == LAO_ID &&
          pendingEntities[0].objectType == Objects.ROLL_CALL &&
          pendingEntities[0].election == null &&
          pendingEntities[0].meeting == null &&
          pendingEntities[0].rollCall == ROLL_CALL
      }
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()
  }

  @Test
  fun removePendingTest() {
    val testObserver = pendingDao.insert(ROLL_CALL_ENTITY).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    val testObserver2 = pendingDao.getPendingObjectsFromLao(LAO_ID).test()
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()

    val testObserver3 = pendingDao.removePendingObject(MESSAGE_ID).test()
    testObserver3.awaitTerminalEvent()
    testObserver3.assertComplete()

    val testObserver4: TestObserver<List<PendingEntity>?> =
      pendingDao.getPendingObjectsFromLao(LAO_ID).test().assertValue { obj: List<PendingEntity?> ->
        obj.isEmpty()
      }
    testObserver4.awaitTerminalEvent()
    testObserver4.assertComplete()
  }

  companion object {
    private val CREATION = Instant.now().epochSecond
    private val LAO_ID = generateLaoId(Base64DataUtils.generatePublicKey(), CREATION, "Lao")
    private val WITNESS = Base64DataUtils.generateKeyPair().publicKey
    private val WITNESS_ENTITY = WitnessEntity(LAO_ID, WITNESS)
    private val MESSAGE_ID = Base64DataUtils.generateMessageID()
    private val WITNESS_MESSAGE = WitnessMessage(MESSAGE_ID)
    private val WITNESSING_ENTITY = WitnessingEntity(LAO_ID, WITNESS_MESSAGE)
    private val ROLL_CALL =
      RollCall(
        LAO_ID,
        LAO_ID,
        "title",
        CREATION,
        CREATION + 10,
        CREATION + 20,
        EventState.CREATED,
        LinkedHashSet(),
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
    private val ROLL_CALL_ENTITY = PendingEntity(MESSAGE_ID, LAO_ID, ROLL_CALL)
    private val ELECTION_ENTITY =
      PendingEntity(Base64DataUtils.generateMessageIDOtherThan(MESSAGE_ID), LAO_ID, ELECTION)
    private val MEETING_ENTITY =
      PendingEntity(
        Base64DataUtils.generateMessageIDOtherThan(ELECTION_ENTITY.messageID),
        LAO_ID,
        MEETING
      )
  }
}
