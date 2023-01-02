package com.github.dedis.popstellar.utility.handler;

import com.github.dedis.popstellar.di.DataRegistryModuleHelper;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.DataRegistry;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.*;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.digitalcash.TransactionObject;
import com.github.dedis.popstellar.model.objects.security.*;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MessageRepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.*;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.lenient;

@RunWith(MockitoJUnitRunner.class)
public class TransactionCoinHandlerTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();
  private static final CreateLao CREATE_LAO = new CreateLao("lao", SENDER);

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
  private RollCall rollCall;

  private LAORepository laoRepo;
  private MessageHandler messageHandler;
  private Channel coinChannel;
  private Gson gson;

  private PostTransactionCoin postTransactionCoin;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, DataHandlingException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    postTransactionCoin = new PostTransactionCoin(TRANSACTION);

    laoRepo = new LAORepository();
    DataRegistry dataRegistry = DataRegistryModuleHelper.buildRegistry(laoRepo, keyManager);
    MessageRepository messageRepo = new MessageRepository();
    gson = JsonModule.provideGson(dataRegistry);
    messageHandler = new MessageHandler(messageRepo, dataRegistry);

    // Create one LAO
    lao = new Lao(CREATE_LAO.getName(), CREATE_LAO.getOrganizer(), CREATE_LAO.getCreation());
    lao.setLastModified(lao.getCreation());

    // Create one Roll Call and add it to the LAO
    // rollCall = new RollCall(lao.getId(), Instant.now().getEpochSecond(), "roll call 1");

    // Add the LAO to the LAORepository
    laoRepo.updateLao(lao);

    // Add the CreateLao message to the LAORepository
    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, gson);
    messageRepo.addMessage(createLaoMessage);

    CloseRollCall closeRollCall =
        new CloseRollCall(
            CREATE_LAO.getId(),
            RollCall.generateCreateRollCallId(
                lao.getId(), Instant.now().getEpochSecond(), "roll call 1"),
            Long.MAX_VALUE,
            new ArrayList<>());
    MessageGeneral message = new MessageGeneral(SENDER_KEY, closeRollCall, gson);
    messageRepo.addMessage(message);
    coinChannel = lao.getChannel().subChannel("coin").subChannel(SENDER.getEncoded());
  }

  @Test
  public void testHandlePostTransactionCoin()
      throws DataHandlingException, UnknownLaoException, UnknownRollCallException,
          UnknownElectionException {
    MessageGeneral message = new MessageGeneral(SENDER_KEY, postTransactionCoin, gson);
    messageHandler.handleMessage(messageSender, coinChannel, message);

    Lao updatedLao = laoRepo.getLaoViewByChannel(lao.getChannel()).createLaoCopy();

    assertEquals(1, updatedLao.getTransactionByUser().size());
    assertEquals(1, updatedLao.getTransactionHistoryByUser().size());
    Set<TransactionObject> transactionObjects =
        updatedLao.getTransactionByUser().get(SENDER_KEY.getPublicKey());
    assertTrue(
        transactionObjects.stream()
            .anyMatch(transactionObject -> transactionObject.getChannel().equals(coinChannel)));
    assertEquals(1, lao.getPubKeyByHash().size());
  }
}
