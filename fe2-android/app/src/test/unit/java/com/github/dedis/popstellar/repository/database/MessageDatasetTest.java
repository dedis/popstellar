package com.github.dedis.popstellar.repository.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.AppDatabaseModuleHelper;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.Gson;

import org.junit.*;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;

@HiltAndroidTest
@RunWith(AndroidJUnit4.class)
public class MessageDatasetTest {

  @Inject Gson gson;
  private static AppDatabase appDatabase;
  private static MessageDao messageDao;

  @Rule public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

  @Before
  public void before() {
    hiltRule.inject();
    appDatabase =
        AppDatabaseModuleHelper.getAppDatabase(ApplicationProvider.getApplicationContext());
    messageDao = appDatabase.messageDao();
  }

  @After
  public void close() {
    appDatabase.close();
  }

  @Test
  public void insertAndGetTest() {
    MessageID messageID = Base64DataUtils.generateMessageID();
    Data data = new CreateLao("name", Base64DataUtils.generatePublicKey());
    MessageGeneral messageGeneral =
        new MessageGeneral(Base64DataUtils.generateKeyPair(), data, gson);
    MessageEntity message = new MessageEntity(messageID, messageGeneral);
    TestObserver<Void> testObserver = messageDao.insert(message).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    // Retrieve and test that the message is the same
    assertEquals(message.getMessageId(), messageDao.getMessageById(messageID).getMessageId());
  }

  @Test
  public void insertNullContentTest() {
    MessageID messageID = Base64DataUtils.generateMessageID();
    MessageEntity message = new MessageEntity(messageID, null);
    TestObserver<Void> testObserver = messageDao.insert(message).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    // Retrieve and test that the message is the same
    assertEquals(message, messageDao.getMessageById(messageID));
  }

  @Test
  public void insertWithSameIdReplaceTest() {
    MessageID messageID = Base64DataUtils.generateMessageID();
    MessageEntity message = new MessageEntity(messageID, null);
    TestObserver<Void> testObserver = messageDao.insert(message).test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    TestObserver<Void> testObserver2 = messageDao.insert(message).test();

    testObserver2.awaitTerminalEvent();
    testObserver2.assertComplete();

    // Check that there's only 1 element
    messageDao
        .takeFirstNMessages(2)
        .test()
        .assertValue(
            messageEntities ->
                messageEntities.size() == 1
                    && messageEntities.get(0).getMessageId().equals(messageID));
  }

  @Test
  public void retrieveFirstNMessagesTest() {
    MessageID messageID1 = Base64DataUtils.generateMessageID();
    MessageID messageID2 = Base64DataUtils.generateMessageID();
    MessageID messageID3 = Base64DataUtils.generateMessageID();

    MessageEntity message1 = new MessageEntity(messageID1, null);
    MessageEntity message2 = new MessageEntity(messageID2, null);
    MessageEntity message3 = new MessageEntity(messageID3, null);

    TestObserver<Void> testObserver =
        messageDao
            .insert(message1)
            .andThen(messageDao.insert(message2))
            .andThen(messageDao.insert(message3))
            .test();

    testObserver.awaitTerminalEvent();
    testObserver.assertComplete();

    messageDao
        .takeFirstNMessages(3)
        .test()
        .assertValue(
            messageEntities ->
                messageEntities.size() == 3
                    && messageEntities.get(0).getMessageId().equals(messageID1)
                    && messageEntities.get(1).getMessageId().equals(messageID2)
                    && messageEntities.get(2).getMessageId().equals(messageID3));
  }
}
