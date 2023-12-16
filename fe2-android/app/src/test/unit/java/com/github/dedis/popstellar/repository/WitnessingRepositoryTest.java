package com.github.dedis.popstellar.repository;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.digitalcash.HashDao;
import com.github.dedis.popstellar.repository.database.digitalcash.TransactionDao;
import com.github.dedis.popstellar.repository.database.event.election.ElectionDao;
import com.github.dedis.popstellar.repository.database.event.meeting.MeetingDao;
import com.github.dedis.popstellar.repository.database.event.rollcall.RollCallDao;
import com.github.dedis.popstellar.repository.database.witnessing.*;
import com.github.dedis.popstellar.utility.error.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.*;

import io.reactivex.Completable;
import io.reactivex.Single;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class WitnessingRepositoryTest {

  @Mock private static AppDatabase appDatabase;
  @Mock private static RollCallDao rollCallDao;
  @Mock private static WitnessDao witnessDao;
  @Mock private static WitnessingDao witnessingDao;
  @Mock private static PendingDao pendingDao;
  @Mock private static TransactionDao transactionDao;
  @Mock private static HashDao hashDao;
  @Mock private static ElectionDao electionDao;
  @Mock private static MeetingDao meetingDao;
  private static WitnessingRepository witnessingRepository;
  private static RollCallRepository rollCallRepo;
  private static ElectionRepository electionRepo;
  private static MeetingRepository meetingRepo;
  private static final String LAO_ID = "LAO_ID";
  private static final MessageID MESSAGE_ID = generateMessageID();
  private static final PublicKey WITNESS = generateKeyPair().publicKey;
  private static final Set<PublicKey> WITNESSES = new HashSet<>(Collections.singletonList(WITNESS));
  private static final WitnessMessage WITNESS_MESSAGE = new WitnessMessage(MESSAGE_ID);
  private static final long CREATION = Instant.now().getEpochSecond();
  private static final RollCall ROLL_CALL =
      new RollCall(
          LAO_ID,
          LAO_ID,
          "title",
          CREATION,
          CREATION + 10,
          CREATION + 20,
          EventState.CREATED,
          new HashSet<>(),
          "loc",
          "");
  private static final Election ELECTION =
      new Election.ElectionBuilder(LAO_ID, 100321014, "Election")
          .setElectionVersion(ElectionVersion.OPEN_BALLOT)
          .build();
  private static final Meeting MEETING =
      new Meeting(
          Meeting.generateCreateMeetingId(LAO_ID, CREATION, "name"),
          "name",
          CREATION,
          CREATION,
          CREATION,
          "",
          CREATION,
          "",
          new ArrayList<>());

  @Rule(order = 0)
  public final MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    Application application = ApplicationProvider.getApplicationContext();
    when(appDatabase.witnessDao()).thenReturn(witnessDao);
    when(appDatabase.witnessingDao()).thenReturn(witnessingDao);
    when(appDatabase.pendingDao()).thenReturn(pendingDao);
    when(appDatabase.rollCallDao()).thenReturn(rollCallDao);
    when(appDatabase.electionDao()).thenReturn(electionDao);
    when(appDatabase.meetingDao()).thenReturn(meetingDao);
    when(appDatabase.transactionDao()).thenReturn(transactionDao);
    when(appDatabase.hashDao()).thenReturn(hashDao);

    rollCallRepo = new RollCallRepository(appDatabase, application);
    electionRepo = new ElectionRepository(appDatabase, application);
    meetingRepo = new MeetingRepository(appDatabase, application);
    DigitalCashRepository digitalCashRepo = new DigitalCashRepository(appDatabase, application);
    witnessingRepository =
        new WitnessingRepository(
            appDatabase, application, rollCallRepo, electionRepo, meetingRepo, digitalCashRepo);

    when(witnessDao.insertAll(anyList())).thenReturn(Completable.complete());
    when(witnessDao.getWitnessesByLao(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(witnessDao.isWitness(anyString(), any())).thenReturn(0);

    when(witnessingDao.insert(any())).thenReturn(Completable.complete());
    when(witnessingDao.getWitnessMessagesByLao(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(witnessingDao.deleteMessagesByIds(anyString(), anySet()))
        .thenReturn(Completable.complete());

    when(pendingDao.insert(any())).thenReturn(Completable.complete());
    when(pendingDao.getPendingObjectsFromLao(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(pendingDao.removePendingObject(any())).thenReturn(Completable.complete());

    when(rollCallDao.insert(any())).thenReturn(Completable.complete());
    when(rollCallDao.getRollCallsByLaoId(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));

    when(electionDao.getElectionsByLaoId(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(electionDao.insert(any())).thenReturn(Completable.complete());

    when(meetingDao.insert(any())).thenReturn(Completable.complete());
    when(meetingDao.getMeetingsByLaoId(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));

    when(hashDao.getDictionaryByLaoId(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(hashDao.deleteByLaoId(anyString())).thenReturn(Completable.complete());
    when(hashDao.insertAll(anyList())).thenReturn(Completable.complete());
    when(transactionDao.getTransactionsByLaoId(anyString()))
        .thenReturn(Single.just(Collections.emptyList()));
    when(transactionDao.deleteByLaoId(anyString())).thenReturn(Completable.complete());
    when(transactionDao.insert(any())).thenReturn(Completable.complete());

    witnessingRepository.addWitnesses(LAO_ID, WITNESSES);
    witnessingRepository.addWitnessMessage(LAO_ID, WITNESS_MESSAGE);
  }

  @After
  public void tearDown() {
    appDatabase.close();
  }

  @Test
  public void achieveSignatureThresholdPerformActionRollCall() throws UnknownRollCallException {
    PendingEntity pendingEntity = new PendingEntity(MESSAGE_ID, LAO_ID, ROLL_CALL);
    witnessingRepository.addPendingEntity(pendingEntity);
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS);

    // Verify the roll call has been added to the repo
    assertEquals(ROLL_CALL, rollCallRepo.getRollCallWithId(LAO_ID, ROLL_CALL.id));
  }

  @Test
  public void achieveSignatureThresholdPerformActionElection() throws UnknownElectionException {
    PendingEntity pendingEntity = new PendingEntity(MESSAGE_ID, LAO_ID, ELECTION);
    witnessingRepository.addPendingEntity(pendingEntity);
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS);

    // Verify the election has been added to the repo
    assertEquals(ELECTION, electionRepo.getElection(LAO_ID, ELECTION.id));
  }

  @Test
  public void achieveSignatureThresholdPerformActionMeeting() throws UnknownMeetingException {
    PendingEntity pendingEntity = new PendingEntity(MESSAGE_ID, LAO_ID, MEETING);
    witnessingRepository.addPendingEntity(pendingEntity);
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS);

    // Verify the meeting has been added to the repo
    assertEquals(MEETING, meetingRepo.getMeetingWithId(LAO_ID, MEETING.id));
  }
}
