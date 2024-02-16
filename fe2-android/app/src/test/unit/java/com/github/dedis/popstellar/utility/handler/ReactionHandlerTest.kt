package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddReaction
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteReaction
import com.github.dedis.popstellar.model.objects.Lao
import com.github.dedis.popstellar.model.objects.Reaction
import com.github.dedis.popstellar.repository.LAORepository
import com.github.dedis.popstellar.repository.SocialMediaRepository
import com.github.dedis.popstellar.repository.database.AppDatabase
import com.github.dedis.popstellar.repository.database.lao.LAODao
import com.github.dedis.popstellar.repository.remote.MessageSender
import com.github.dedis.popstellar.testutils.Base64DataUtils
import com.github.dedis.popstellar.testutils.MockitoKotlinHelpers
import com.github.dedis.popstellar.utility.error.DataHandlingException
import com.github.dedis.popstellar.utility.error.InvalidMessageIdException
import com.github.dedis.popstellar.utility.error.UnknownLaoException
import com.github.dedis.popstellar.utility.handler.data.HandlerContext
import com.github.dedis.popstellar.utility.handler.data.ReactionHandler
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ReactionHandlerTest {
  private lateinit var handler: ReactionHandler

  @Mock lateinit var appDatabase: AppDatabase

  @Mock lateinit var laoDao: LAODao

  @Mock lateinit var messageSender: MessageSender

  @Mock lateinit var socialMediaRepo: SocialMediaRepository

  @Before
  @Throws(
    GeneralSecurityException::class,
    DataHandlingException::class,
    IOException::class,
    UnknownLaoException::class
  )
  fun setup() {
    MockitoAnnotations.openMocks(this)
    val application = ApplicationProvider.getApplicationContext<Application>()

    Mockito.`when`(appDatabase.laoDao()).thenReturn(laoDao)
    Mockito.`when`(laoDao.allLaos).thenReturn(Single.just(ArrayList()))
    Mockito.`when`(laoDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())

    val laoRepo = LAORepository(appDatabase, application)
    laoRepo.updateLao(LAO)
    handler = ReactionHandler(laoRepo, socialMediaRepo)
  }

  @Test
  @Throws(UnknownLaoException::class, InvalidMessageIdException::class)
  fun testHandleAddReaction() {
    Mockito.`when`(
        socialMediaRepo.addReaction(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
      )
      .thenReturn(true)
    val ctx = HandlerContext(REACTION_ID, SENDER, CHANNEL, messageSender)

    handler.handleAddReaction(ctx, ADD_REACTION)
    Mockito.verify(socialMediaRepo).addReaction(LAO_ID, REACTION)
    Mockito.verifyNoMoreInteractions(socialMediaRepo)
  }

  @Test
  @Throws(UnknownLaoException::class, InvalidMessageIdException::class)
  fun testHandleDeleteReaction() {
    // Act as if reaction exist
    Mockito.`when`(
        socialMediaRepo.deleteReaction(MockitoKotlinHelpers.any(), MockitoKotlinHelpers.any())
      )
      .thenReturn(true)
    val ctx = HandlerContext(REACTION_ID, SENDER, CHANNEL, messageSender)

    handler.handleDeleteReaction(ctx, DELETE_REACTION)
    Mockito.verify(socialMediaRepo).deleteReaction(LAO_ID, REACTION_ID)
    Mockito.verifyNoMoreInteractions(socialMediaRepo)
  }

  @Test
  fun testHandleUnorderedDeleteReaction() {
    // Act as if chirp does not exist
    val ctx = HandlerContext(REACTION_ID, SENDER, CHANNEL, messageSender)

    Assert.assertThrows(InvalidMessageIdException::class.java) {
      handler.handleDeleteReaction(ctx, DELETE_REACTION)
    }
    Mockito.verify(socialMediaRepo).deleteReaction(LAO_ID, REACTION_ID)
    Mockito.verifyNoMoreInteractions(socialMediaRepo)
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private val CREATION_TIME = Instant.now().epochSecond
    private val DELETION_TIME = CREATION_TIME + 10
    private val LAO = Lao("laoName", SENDER, CREATION_TIME)
    private val LAO_ID = LAO.id
    private val CHANNEL = LAO.channel.subChannel("social").subChannel("reactions")
    private const val EMOJI = "\uD83D\uDC4D"
    private val CHIRP_ID = Base64DataUtils.generateMessageID()
    private val REACTION_ID = Base64DataUtils.generateMessageIDOtherThan(CHIRP_ID)
    private val REACTION = Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, CREATION_TIME)
    private val ADD_REACTION = AddReaction(EMOJI, CHIRP_ID, CREATION_TIME)
    private val DELETE_REACTION = DeleteReaction(REACTION_ID, DELETION_TIME)
  }
}
