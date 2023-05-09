package com.github.dedis.popstellar.utility.handler;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.KeyException;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

import io.reactivex.Completable;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePoPToken;
import static com.github.dedis.popstellar.utility.handler.data.MeetingHandler.createMeetingWitnessMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class MeetingHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final PoPToken POP_TOKEN = generatePoPToken();

  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER, new ArrayList<>());
  private static final Channel LAO_CHANNEL = Channel.getLaoChannel(CREATE_LAO.getId());
  private static Lao LAO;

  private static LAORepository laoRepo;
  private static MeetingRepository meetingRepo;
  private static MessageHandler messageHandler;
  private static Gson gson;

  private static Meeting meeting;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Rule public InstantTaskExecutorRule rule = new InstantTaskExecutorRule();

  @Before
  public void setup()
      throws GeneralSecurityException, IOException, KeyException, UnknownRollCallException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);
    lenient().when(keyManager.getValidPoPToken(any(), any())).thenReturn(POP_TOKEN);

    lenient().when(messageSender.subscribe(any())).then(args -> Completable.complete());

    laoRepo = new LAORepository();
    meetingRepo = new MeetingRepository();

    DataRegistry dataRegistry =
        DataRegistryModuleHelper.buildRegistry(laoRepo, keyManager, meetingRepo);
    MessageRepository messageRepo = new MessageRepository();
    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO
    LAO = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    LAO.setLastModified(LAO.getCreation());

    // Create one Roll Call and add it to the roll call repo
    long now = Instant.now().getEpochSecond();
    String name = "NAME";
    String ID = Meeting.generateCreateMeetingId(LAO.getId(), now, name);
    meeting = new Meeting(ID, name, now, now + 1, now + 2, "", now, "", new ArrayList<>());
    meetingRepo.updateMeeting(LAO.getId(), meeting);

    // Add the LAO to the LAORepository
    laoRepo.updateLao(LAO);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage);
  }

  @Test
  public void handleCreateMeetingTest()
      throws UnknownElectionException, UnknownRollCallException, UnknownLaoException,
          DataHandlingException, NoRollCallException, UnknownMeetingException {
    // Create the create Meeting message
    CreateMeeting createMeeting =
        new CreateMeeting(
            LAO.getId(),
            meeting.getId(),
            meeting.getName(),
            meeting.getCreation(),
            meeting.getLocation(),
            meeting.getStartTimestamp(),
            meeting.getEndTimestamp());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, createMeeting, gson);

    // Call the message handler
    messageHandler.handleMessage(messageSender, LAO_CHANNEL, message);

    // Check the new Meeting is present with state CREATED and the correct ID
    Meeting meetingSearch = meetingRepo.getMeetingWithId(LAO.getId(), meeting.getId());
    assertEquals(createMeeting.getId(), meetingSearch.getId());

    // Check the WitnessMessage has been created
    Optional<WitnessMessage> witnessMessage =
        laoRepo.getLaoByChannel(LAO_CHANNEL).getWitnessMessage(message.getMessageId());
    assertTrue(witnessMessage.isPresent());

    // Check the Witness message contains the expected title and description
    WitnessMessage expectedMessage =
        createMeetingWitnessMessage(message.getMessageId(), meetingSearch);
    assertEquals(expectedMessage.getTitle(), witnessMessage.get().getTitle());
    assertEquals(expectedMessage.getDescription(), witnessMessage.get().getDescription());
  }
}
