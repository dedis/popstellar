package com.github.dedis.popstellar.utility.handler;

import android.app.Application;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.DigitalCashRepository;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.repository.database.AppDatabase;
import com.github.dedis.popstellar.repository.database.digitalcash.*;
import com.github.dedis.popstellar.repository.database.message.MessageDao;
import com.github.dedis.popstellar.repository.database.message.MessageEntity;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.NoRollCallException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

import io.reactivex.Completable;
import io.reactivex.Single;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class TransactionCoinHandlerTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER, new ArrayList<>());

  // Version
  private static final int VERSION = 1;

  // Creation TxOut
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = SENDER.getEncoded();
  private static final String SIG = "CAFEBABE";
  private static final ScriptInput SCRIPTTXIN =
      new ScriptInput(TYPE, new PublicKey(PUBKEY), new Signature(SIG));
  private static final Input TXIN = new Input(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);

  // Creation TXOUT
  private static final int VALUE = 32;
  private static final String PUBKEYHASH = SENDER.computeHash();
  private static final ScriptOutput SCRIPT_TX_OUT = new ScriptOutput(TYPE, PUBKEYHASH);
  private static final Output TXOUT = new Output(VALUE, SCRIPT_TX_OUT);

  // List TXIN, List TXOUT
  private static final List<Input> TX_INS = Collections.singletonList(TXIN);
  private static final List<Output> TX_OUTS = Collections.singletonList(TXOUT);

  // Locktime
  private static final long TIMESTAMP = 0;

  // Transaction
  private static final Transaction TRANSACTION =
      new Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP);

  private Lao lao;

  private DigitalCashRepository digitalCashRepo;
  private MessageHandler messageHandler;
  private Channel coinChannel;
  private Gson gson;

  private PostTransactionCoin postTransactionCoin;

  @Mock AppDatabase appDatabase;
  @Mock MessageDao messageDao;
  @Mock TransactionDao transactionDao;
  @Mock HashDao hashDao;
  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup()
      throws GeneralSecurityException, DataHandlingException, IOException, UnknownRollCallException,
          UnknownLaoException, NoRollCallException {
    MockitoAnnotations.openMocks(this);
    Application application = ApplicationProvider.getApplicationContext();

    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);
    lenient().when(messageSender.subscribe(any())).then(args -> Completable.complete());

    when(appDatabase.messageDao()).thenReturn(messageDao);
    when(messageDao.takeFirstNMessages(anyInt())).thenReturn(Single.just(new ArrayList<>()));
    when(messageDao.insert(any(MessageEntity.class))).thenReturn(Completable.complete());
    when(messageDao.getMessageById(any(MessageID.class))).thenReturn(null);

    when(appDatabase.transactionDao()).thenReturn(transactionDao);
    when(transactionDao.getTransactionsByLaoId(anyString()))
        .thenReturn(Single.just(new ArrayList<>()));
    when(transactionDao.insert(any(TransactionEntity.class))).thenReturn(Completable.complete());
    when(transactionDao.deleteByLaoId(anyString())).thenReturn(Completable.complete());

    when(appDatabase.hashDao()).thenReturn(hashDao);
    when(hashDao.getDictionaryByLaoId(anyString())).thenReturn(Single.just(new ArrayList<>()));
    when(hashDao.insertAll(any())).thenReturn(Completable.complete());
    when(hashDao.deleteByLaoId(anyString())).thenReturn(Completable.complete());

    postTransactionCoin = new PostTransactionCoin(TRANSACTION);

    digitalCashRepo = new DigitalCashRepository(appDatabase, application);
    MessageRepository messageRepo = new MessageRepository(appDatabase, application);

    DataRegistry dataRegistry = DataRegistryModuleHelper.buildRegistry(digitalCashRepo, keyManager);

    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());

    digitalCashRepo.initializeDigitalCash(lao.getId(), Collections.singletonList(SENDER));
    coinChannel = lao.getChannel().subChannel("coin").subChannel(SENDER.getEncoded());
  }

  @Test
  public void testHandlePostTransactionCoin()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException, NoRollCallException {
    MessageGeneral message = new MessageGeneral(SENDER_KEY, postTransactionCoin, gson);
    messageHandler.handleMessage(messageSender, coinChannel, message);

    List<TransactionObject> transactions = digitalCashRepo.getTransactions(lao.getId(), SENDER);
    assertEquals(1, transactions.size());
    assertTrue(
        transactions.stream()
            .anyMatch(transactionObject -> transactionObject.getChannel().equals(coinChannel)));
  }
}
