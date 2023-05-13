package com.github.dedis.popstellar.utility.handler;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddReaction;
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteReaction;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.SocialMediaRepository;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.handler.data.HandlerContext;
import com.github.dedis.popstellar.utility.handler.data.ReactionHandler;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;

import dagger.hilt.android.testing.HiltAndroidTest;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class ReactionHandlerTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final long CREATION_TIME = Instant.now().getEpochSecond();
  private static final long DELETION_TIME = CREATION_TIME + 10;

  private static final Lao LAO = new Lao("laoName", SENDER, CREATION_TIME);
  private static final String LAO_ID = LAO.getId();

  private static final Channel CHANNEL =
      LAO.getChannel().subChannel("social").subChannel("reactions");
  private static final String EMOJI = "\uD83D\uDC4D";
  private static final MessageID CHIRP_ID = generateMessageID();

  private static final MessageID REACTION_ID = generateMessageIDOtherThan(CHIRP_ID);
  private static final Reaction REACTION =
      new Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, CREATION_TIME);
  private static final AddReaction ADD_REACTION = new AddReaction(EMOJI, CHIRP_ID, CREATION_TIME);
  private static final DeleteReaction DELETE_REACTION =
      new DeleteReaction(REACTION_ID, DELETION_TIME);

  private ReactionHandler handler;
  private AppDatabase appDatabase;

  @Mock MessageSender messageSender;
  @Mock SocialMediaRepository socialMediaRepo;

  @Before
  public void setup()
      throws GeneralSecurityException, DataHandlingException, IOException, UnknownLaoException {
    MockitoAnnotations.openMocks(this);
    Context context = ApplicationProvider.getApplicationContext();
    appDatabase = AppDatabaseModuleHelper.getAppDatabase(context);
    LAORepository laoRepo =
        new LAORepository(appDatabase, ApplicationProvider.getApplicationContext());
    laoRepo.updateLao(LAO);
    handler = new ReactionHandler(laoRepo, socialMediaRepo);
  }

  @After
  public void tearDown() {
    appDatabase.clearAllTables();
    appDatabase.close();
  }

  @Test
  public void testHandleAddReaction() throws UnknownLaoException, InvalidMessageIdException {
    when(socialMediaRepo.addReaction(any(), any())).thenReturn(true);
    HandlerContext ctx = new HandlerContext(REACTION_ID, SENDER, CHANNEL, messageSender);

    handler.handleAddReaction(ctx, ADD_REACTION);

    verify(socialMediaRepo).addReaction(LAO_ID, REACTION);
    verifyNoMoreInteractions(socialMediaRepo);
  }

  @Test
  public void testHandleDeleteReaction() throws UnknownLaoException, InvalidMessageIdException {
    // Act as if reaction exist
    when(socialMediaRepo.deleteReaction(any(), any())).thenReturn(true);
    HandlerContext ctx = new HandlerContext(REACTION_ID, SENDER, CHANNEL, messageSender);

    handler.handleDeleteReaction(ctx, DELETE_REACTION);

    verify(socialMediaRepo).deleteReaction(LAO_ID, REACTION_ID);
    verifyNoMoreInteractions(socialMediaRepo);
  }

  @Test
  public void testHandleUnorderedDeleteReaction() {
    // Act as if chirp does not exist
    HandlerContext ctx = new HandlerContext(REACTION_ID, SENDER, CHANNEL, messageSender);

    assertThrows(
        InvalidMessageIdException.class, () -> handler.handleDeleteReaction(ctx, DELETE_REACTION));

    verify(socialMediaRepo).deleteReaction(LAO_ID, REACTION_ID);
    verifyNoMoreInteractions(socialMediaRepo);
  }
}
