package com.github.dedis.popstellar.repository;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVersion;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity;
import com.github.dedis.popstellar.utility.error.*;

import org.junit.*;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.*;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class WitnessingRepositoryTest {

  private static AppDatabase appDatabase;
  private static WitnessingRepository witnessingRepository;
  private static RollCallRepository rollCallRepo;
  private static ElectionRepository electionRepo;
  private static MeetingRepository meetingRepo;
  private static final String LAO_ID = "LAO_ID";
  private static final MessageID MESSAGE_ID = generateMessageID();
  private static final PublicKey WITNESS = generateKeyPair().getPublicKey();
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

  @Before
  public void setUp() {
    Application application = ApplicationProvider.getApplicationContext();
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());

    rollCallRepo = new RollCallRepository(appDatabase, application);
    electionRepo = new ElectionRepository(appDatabase, application);
    meetingRepo = new MeetingRepository(appDatabase, application);
    DigitalCashRepository digitalCashRepo = new DigitalCashRepository(appDatabase, application);
    witnessingRepository =
        new WitnessingRepository(
            appDatabase, application, rollCallRepo, electionRepo, meetingRepo, digitalCashRepo);

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
    assertEquals(ROLL_CALL, rollCallRepo.getRollCallWithId(LAO_ID, ROLL_CALL.getId()));
  }

  @Test
  public void achieveSignatureThresholdPerformActionElection() throws UnknownElectionException {
    PendingEntity pendingEntity = new PendingEntity(MESSAGE_ID, LAO_ID, ELECTION);
    witnessingRepository.addPendingEntity(pendingEntity);
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS);

    // Verify the election has been added to the repo
    assertEquals(ELECTION, electionRepo.getElection(LAO_ID, ELECTION.getId()));
  }

  @Test
  public void achieveSignatureThresholdPerformActionMeeting() throws UnknownMeetingException {
    PendingEntity pendingEntity = new PendingEntity(MESSAGE_ID, LAO_ID, MEETING);
    witnessingRepository.addPendingEntity(pendingEntity);
    witnessingRepository.addWitnessToMessage(LAO_ID, MESSAGE_ID, WITNESS);

    // Verify the meeting has been added to the repo
    assertEquals(MEETING, meetingRepo.getMeetingWithId(LAO_ID, MEETING.getId()));
  }
}
