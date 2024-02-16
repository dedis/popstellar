package com.github.dedis.popstellar.repository.database

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.dedis.popstellar.di.AppDatabaseModuleHelper.getAppDatabase
import com.github.dedis.popstellar.model.network.JsonTestUtils
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral
import com.github.dedis.popstellar.model.network.method.message.data.Data
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao
import com.github.dedis.popstellar.repository.database.message.MessageDao
import com.github.dedis.popstellar.repository.database.message.MessageEntity
import com.github.dedis.popstellar.testutils.Base64DataUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageDatabaseTest {
  private lateinit var appDatabase: AppDatabase
  private lateinit var messageDao: MessageDao

  @Before
  fun before() {
    appDatabase = getAppDatabase(ApplicationProvider.getApplicationContext())
    messageDao = appDatabase.messageDao()
  }

  @After
  fun close() {
    appDatabase.close()
  }

  @Test
  fun insertAndGetTest() {
    val messageID = Base64DataUtils.generateMessageID()
    val data: Data = CreateLao("name", Base64DataUtils.generatePublicKey(), ArrayList())
    val messageGeneral = MessageGeneral(Base64DataUtils.generateKeyPair(), data, JsonTestUtils.GSON)
    val message = MessageEntity(messageID, messageGeneral)
    val testObserver = messageDao.insert(message).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    // Retrieve and test that the message is the same
    val messageEntity = messageDao.getMessageById(messageID)
    Assert.assertNotNull(messageEntity)
    Assert.assertEquals(message.messageId, messageEntity!!.messageId)
  }

  @Test
  fun insertNullContentTest() {
    val messageID = Base64DataUtils.generateMessageID()
    val message = MessageEntity(messageID, null)
    val testObserver = messageDao.insert(message).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    // Retrieve and test that the message is the same
    Assert.assertEquals(message, messageDao.getMessageById(messageID))
  }

  @Test
  fun insertWithSameIdReplaceTest() {
    val messageID = Base64DataUtils.generateMessageID()
    val message = MessageEntity(messageID, null)
    val testObserver = messageDao.insert(message).test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()
    val testObserver2 = messageDao.insert(message).test()
    testObserver2.awaitTerminalEvent()
    testObserver2.assertComplete()

    // Check that there's only 1 element
    messageDao.takeFirstNMessages(2).test().assertValue { messageEntities: List<MessageEntity> ->
      (messageEntities.size == 1 && messageEntities[0].messageId == messageID)
    }
  }

  @Test
  fun retrieveFirstNMessagesTest() {
    val messageID1 = Base64DataUtils.generateMessageID()
    val messageID2 = Base64DataUtils.generateMessageID()
    val messageID3 = Base64DataUtils.generateMessageID()
    val message1 = MessageEntity(messageID1, null)
    val message2 = MessageEntity(messageID2, null)
    val message3 = MessageEntity(messageID3, null)
    val testObserver =
      messageDao
        .insert(message1)
        .andThen(messageDao.insert(message2))
        .andThen(messageDao.insert(message3))
        .test()
    testObserver.awaitTerminalEvent()
    testObserver.assertComplete()

    messageDao.takeFirstNMessages(3).test().assertValue { messageEntities: List<MessageEntity> ->
      (messageEntities.size == 3 &&
        messageEntities[0].messageId == messageID1 &&
        messageEntities[1].messageId == messageID2 &&
        messageEntities[2].messageId == messageID3)
    }
  }
}
