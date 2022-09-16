package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.SocialMediaRepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.handler.data.ChirpHandler;
import com.github.dedis.popstellar.utility.handler.data.HandlerContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;

import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@HiltAndroidTest
@RunWith(MockitoJUnitRunner.class)
public class ChirpHandlerTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final long CREATION_TIME = 1631280815;
  private static final long DELETION_TIME = 1642244760;

  private static final Lao LAO = new Lao("laoName", SENDER, CREATION_TIME);
  private static final String LAO_ID = LAO.getId();

  private static final Channel CHIRP_CHANNEL =
      LAO.getChannel().subChannel("social").subChannel(SENDER.getEncoded());

  private static final String TEXT = "textOfTheChirp";
  private static final MessageID PARENT_ID = generateMessageID();

  private static final MessageID CHIRP_ID = generateMessageID();
  private static final Chirp CHIRP = new Chirp(CHIRP_ID, SENDER, TEXT, CREATION_TIME, PARENT_ID);

  private static final AddChirp ADD_CHIRP = new AddChirp(TEXT, PARENT_ID, CREATION_TIME);
  private static final DeleteChirp DELETE_CHIRP = new DeleteChirp(CHIRP_ID, DELETION_TIME);

  private ChirpHandler handler;

  @Mock MessageSender messageSender;
  @Mock SocialMediaRepository socialMediaRepo;

  @Before
  public void setup()
      throws GeneralSecurityException, DataHandlingException, IOException, UnknownLaoException {
    // Instantiate the dependencies here such that they are reset for each test
    LAORepository laoRepo = new LAORepository();
    laoRepo.updateLao(LAO);
    handler = new ChirpHandler(laoRepo, socialMediaRepo);
  }

  @Test
  public void testHandleAddChirp() throws UnknownLaoException {
    HandlerContext ctx = new HandlerContext(CHIRP_ID, SENDER, CHIRP_CHANNEL, messageSender);

    handler.handleChirpAdd(ctx, ADD_CHIRP);

    verify(socialMediaRepo).addChirp(eq(LAO_ID), eq(CHIRP));
    verifyNoMoreInteractions(socialMediaRepo);
  }

  @Test
  public void testHandleDeleteChirp() throws UnknownLaoException, InvalidMessageIdException {
    // Act as if chirp exist
    when(socialMediaRepo.deleteChirp(any(), any())).thenReturn(true);
    HandlerContext ctx = new HandlerContext(CHIRP_ID, SENDER, CHIRP_CHANNEL, messageSender);

    handler.handleDeleteChirp(ctx, DELETE_CHIRP);

    verify(socialMediaRepo).deleteChirp(eq(LAO_ID), eq(CHIRP_ID));
    verifyNoMoreInteractions(socialMediaRepo);
  }

  @Test
  public void testUnorderedDeleteChirp() {
    // Act as if chirp does not exist
    when(socialMediaRepo.deleteChirp(anyString(), any())).thenReturn(false);
    HandlerContext ctx = new HandlerContext(CHIRP_ID, SENDER, CHIRP_CHANNEL, messageSender);

    assertThrows(
        InvalidMessageIdException.class, () -> handler.handleDeleteChirp(ctx, DELETE_CHIRP));

    verify(socialMediaRepo).deleteChirp(eq(LAO_ID), eq(CHIRP_ID));
    verifyNoMoreInteractions(socialMediaRepo);
  }
}
