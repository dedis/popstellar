package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.digitalcash.ScriptOutputObject;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import org.junit.Test;

public class ScriptOutputObjectTest {
  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.publicKey;
  private static final String TYPE = "P2PKH";

  private final String pubKeyHash = SENDER.computeHash();
  private final ScriptOutputObject scriptTxOut = new ScriptOutputObject(TYPE, pubKeyHash);

  @Test
  public void getTypeTest() {
    assertEquals(TYPE, scriptTxOut.type);
  }

  @Test
  public void getPublicKeyHashTest() {
    assertEquals(pubKeyHash, scriptTxOut.pubKeyHash);
  }
}
