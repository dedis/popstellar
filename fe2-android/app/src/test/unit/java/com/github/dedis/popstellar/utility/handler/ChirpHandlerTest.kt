package com.github.dedis.popstellar.utility.handler

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.AddChirp
import com.github.dedis.popstellar.model.network.method.message.data.socialmedia.DeleteChirp
import com.github.dedis.popstellar.model.objects.Chirp
import com.github.dedis.popstellar.model.objects.Lao
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
import com.github.dedis.popstellar.utility.handler.data.ChirpHandler
import com.github.dedis.popstellar.utility.handler.data.HandlerContext
import dagger.hilt.android.testing.HiltAndroidTest
import io.reactivex.Completable
import io.reactivex.Single
import java.io.IOException
import java.security.GeneralSecurityException
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ChirpHandlerTest {
  private lateinit var handler: ChirpHandler

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
    // Instantiate the dependencies here such that they are reset for each test
    MockitoAnnotations.openMocks(this)

    val application = ApplicationProvider.getApplicationContext<Application>()
    Mockito.`when`(appDatabase.laoDao()).thenReturn(laoDao)
    Mockito.`when`(laoDao.allLaos).thenReturn(Single.just(ArrayList()))
    Mockito.`when`(laoDao.insert(MockitoKotlinHelpers.any())).thenReturn(Completable.complete())

    val laoRepo = LAORepository(appDatabase, application)
    laoRepo.updateLao(LAO)

    handler = ChirpHandler(laoRepo, socialMediaRepo)
  }

  @Test
  @Throws(UnknownLaoException::class)
  fun testHandleAddChirp() {
    val ctx = HandlerContext(CHIRP_ID, SENDER, CHIRP_CHANNEL, messageSender)
    handler.handleChirpAdd(ctx, ADD_CHIRP)

    Mockito.verify(socialMediaRepo).addChirp(LAO_ID, CHIRP)
    Mockito.verifyNoMoreInteractions(socialMediaRepo)
  }

  @Test
  @Throws(UnknownLaoException::class, InvalidMessageIdException::class)
  fun testHandleDeleteChirp() {
    // Act as if chirp exist
    Mockito.`when`(
        socialMediaRepo.deleteChirp(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any())
      )
      .thenReturn(true)

    val ctx = HandlerContext(CHIRP_ID, SENDER, CHIRP_CHANNEL, messageSender)
    handler.handleDeleteChirp(ctx, DELETE_CHIRP)

    Mockito.verify(socialMediaRepo).deleteChirp(LAO_ID, CHIRP_ID)
    Mockito.verifyNoMoreInteractions(socialMediaRepo)
  }

  @Test
  fun testUnorderedDeleteChirp() {
    // Act as if chirp does not exist
    Mockito.`when`(
        socialMediaRepo.deleteChirp(ArgumentMatchers.anyString(), MockitoKotlinHelpers.any())
      )
      .thenReturn(false)

    val ctx = HandlerContext(CHIRP_ID, SENDER, CHIRP_CHANNEL, messageSender)

    Assert.assertThrows(InvalidMessageIdException::class.java) {
      handler.handleDeleteChirp(ctx, DELETE_CHIRP)
    }
    Mockito.verify(socialMediaRepo).deleteChirp(LAO_ID, CHIRP_ID)
    Mockito.verifyNoMoreInteractions(socialMediaRepo)
  }

  companion object {
    private val SENDER_KEY = Base64DataUtils.generateKeyPair()
    private val SENDER = SENDER_KEY.publicKey
    private const val CREATION_TIME: Long = 1631280815
    private const val DELETION_TIME: Long = 1642244760
    private val LAO = Lao("laoName", SENDER, CREATION_TIME)
    private val LAO_ID = LAO.id
    private val CHIRP_CHANNEL = LAO.channel.subChannel("social").subChannel(SENDER.encoded)
    private const val TEXT = "textOfTheChirp"
    private val PARENT_ID = Base64DataUtils.generateMessageID()
    private val CHIRP_ID = Base64DataUtils.generateMessageID()
    private val CHIRP = Chirp(CHIRP_ID, SENDER, TEXT, CREATION_TIME, PARENT_ID)
    private val ADD_CHIRP = AddChirp(TEXT, PARENT_ID, CREATION_TIME)
    private val DELETE_CHIRP = DeleteChirp(CHIRP_ID, DELETION_TIME)
  }
}
