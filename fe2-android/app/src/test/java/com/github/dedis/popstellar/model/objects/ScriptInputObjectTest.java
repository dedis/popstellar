package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;

import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;

public class ScriptInputObjectTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

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
    assertEquals(type, scriptTxIn.getType());
  }

  @Test
  public void getPubKeyTest() {
    assertEquals(pubKey, scriptTxIn.getPubKey().getEncoded());
  }

  @Test
  public void getSigTest() {
    assertEquals(sig, scriptTxIn.getSig().getEncoded());
  }
}
