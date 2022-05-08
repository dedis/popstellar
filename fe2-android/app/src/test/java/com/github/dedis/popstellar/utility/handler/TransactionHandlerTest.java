package com.github.dedis.popstellar.utility.handler;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.MessageGeneral;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.PostTransaction;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptTxIn;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.ScriptTxOut;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.Transaction;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.TxIn;
import com.github.dedis.popstellar.model.network.method.message.data.digitalcash.TxOut;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Channel;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.remote.MessageSender;
import com.github.dedis.popstellar.utility.error.DataHandlingException;
import com.github.dedis.popstellar.utility.security.KeyManager;
import com.google.gson.Gson;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.reactivex.Completable;

@RunWith(MockitoJUnitRunner.class)
public class TransactionHandlerTest {
  public static final String TAG = TransactionHandlerTest.class.getSimpleName();

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static final long CREATION_TIME = 1631280815;
  private static final long DELETION_TIME = 1642244760;
  private static final String LAO_NAME = "laoName";
  private static final String LAO_ID = Lao.generateLaoId(SENDER, CREATION_TIME, LAO_NAME);
  private static Lao lao;
  private static Channel cashChannel;

  // Version
  private static final int VERSION = 1;

  // Creation TxOut
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";
  private static final String TYPE = "P2PKH";
  private static final String PUBKEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String SIG = "CAFEBABE";
  private static final ScriptTxIn SCRIPTTXIN = new ScriptTxIn(TYPE, PUBKEY, SIG);
  private static final TxIn TXIN = new TxIn(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);

  // Creation TXOUT
  private static final int VALUE = 32;
  private static final String PUBKEYHASH = "2jmj7l5rSw0yVb-vlWAYkK-YBwk=";
  private static final ScriptTxOut SCRIPT_TX_OUT = new ScriptTxOut(TYPE, PUBKEYHASH);
  private static final TxOut TXOUT = new TxOut(VALUE, SCRIPT_TX_OUT);

  // List TXIN, List TXOUT
  private static final List<TxIn> TX_INS = Collections.singletonList(TXIN);
  private static final List<TxOut> TX_OUTS = Collections.singletonList(TXOUT);

  // Locktime
  private static final long TIMESTAMP = 0;

  // Transaction
  private static final Transaction TRANSACTION =
      new Transaction(VERSION, TX_INS, TX_OUTS, TIMESTAMP);

  // POST TRANSACTION
  private static final PostTransaction POST_TRANSACTION = new PostTransaction(TRANSACTION);
  private static final CreateLao CREATE_LAO =
      new CreateLao(LAO_ID, LAO_NAME, CREATION_TIME, SENDER, new ArrayList<>());

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());

  private LAORepository laoRepository;
  private MessageHandler messageHandler;

  @Mock MessageSender messageSender;
  @Mock KeyManager keyManager;

  @Before
  public void setup() throws GeneralSecurityException, DataHandlingException, IOException {
    lenient().when(keyManager.getMainKeyPair()).thenReturn(SENDER_KEY);
    lenient().when(keyManager.getMainPublicKey()).thenReturn(SENDER);

    when(messageSender.subscribe(any())).then(args -> Completable.complete());

    Channel channel = Channel.getLaoChannel(LAO_ID);

    laoRepository = new LAORepository();
    messageHandler = new MessageHandler(DataRegistryModule.provideDataRegistry(), keyManager);

    MessageGeneral createLaoMessage = new MessageGeneral(SENDER_KEY, CREATE_LAO, GSON);
    messageHandler.handleMessage(laoRepository, messageSender, channel, createLaoMessage);

    lao = laoRepository.getLaoById().get(LAO_ID).getLao();
    cashChannel = lao.getChannel().subChannel("coin").subChannel(SENDER.getEncoded());
  }

  @Test
  public void testHandlePostTransaction() throws DataHandlingException {
    MessageGeneral message = new MessageGeneral(SENDER_KEY, POST_TRANSACTION, GSON);
    messageHandler.handleMessage(laoRepository, messageSender, cashChannel, message);
    Optional<Transaction> transactionOpt = lao.getTransaction();
    assertTrue(transactionOpt.isPresent());
    Transaction transaction = transactionOpt.get();
    assertEquals(VERSION, transaction.getVersion());
    assertEquals(transaction.getTimestamp(), TIMESTAMP);
    assertEquals(transaction.getTxIns().get(0).getTxOutHash(), Tx_OUT_HASH);
    assertEquals(transaction.getTxOuts().get(0).getScript().getType(), TYPE);
  }
}
