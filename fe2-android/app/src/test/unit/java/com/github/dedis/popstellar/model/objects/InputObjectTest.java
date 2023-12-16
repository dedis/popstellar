package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject;
import com.github.dedis.popstellar.model.objects.security.*;
import java.security.GeneralSecurityException;
import org.junit.Before;
import org.junit.Test;

public class InputObjectTest {
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.publicKey;

  private String type;
  private String pubKey;
  private String sig;
  private ScriptInputObject scriptTxIn;
  private InputObject input;

  @Before
  public void setup() throws GeneralSecurityException {
    type = "P2PKH";
    pubKey = SENDER.getEncoded();
    sig = SENDER_KEY.sign(SENDER).getEncoded();
    scriptTxIn = new ScriptInputObject(type, new PublicKey(pubKey), new Signature(sig));
    input = new InputObject(Tx_OUT_HASH, TX_OUT_INDEX, scriptTxIn);
  }

  @Test
  public void getTxOutIndexTest() {
    assertEquals(TX_OUT_INDEX, input.txOutIndex);
  }

  @Test
  public void getTxOutHashTest() {
    assertEquals(Tx_OUT_HASH, input.txOutHash);
  }

  @Test
  public void getScriptTest() {
    assertEquals(input.script.getPubKey().getEncoded(), pubKey);
    assertEquals(input.script.sig.getEncoded(), sig);
    assertEquals(input.script.type, type);
  }
}
