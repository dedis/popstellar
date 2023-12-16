package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptInputObject;
import com.github.dedis.popstellar.model.objects.security.*;
import java.security.GeneralSecurityException;
import org.junit.Before;
import org.junit.Test;

public class ScriptInputObjectTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.publicKey;

  private String type;
  private String pubKey;
  private String sig;
  private ScriptInputObject scriptTxIn;

  @Before
  public void setup() throws GeneralSecurityException {
    type = "P2PKH";
    pubKey = SENDER.getEncoded();
    sig = SENDER_KEY.sign(SENDER).getEncoded();
    scriptTxIn = new ScriptInputObject(type, new PublicKey(pubKey), new Signature(sig));
  }

  @Test
  public void getTypeTest() {
    assertEquals(type, scriptTxIn.type);
  }

  @Test
  public void getPubKeyTest() {
    assertEquals(pubKey, scriptTxIn.getPubKey().getEncoded());
  }

  @Test
  public void getSigTest() {
    assertEquals(sig, scriptTxIn.sig.getEncoded());
  }
}
