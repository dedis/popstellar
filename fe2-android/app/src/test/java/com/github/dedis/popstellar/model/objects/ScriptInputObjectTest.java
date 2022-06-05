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

  private static String TYPE;
  private static String PUBKEY;
  private static String SIG;
  private static ScriptInputObject SCRIPTTXIN;

  @Before
  public void setup() throws GeneralSecurityException {
    TYPE = "P2PKH";
    PUBKEY = SENDER.getEncoded();
    SIG = SENDER_KEY.sign(SENDER).getEncoded();
    SCRIPTTXIN = new ScriptInputObject(TYPE, new PublicKey(PUBKEY), new Signature(SIG));
  }

  @Test
  public void getTypeTest() {
    assertEquals(TYPE, SCRIPTTXIN.getType());
  }

  @Test
  public void getPubKeyTest() {
    assertEquals(PUBKEY, SCRIPTTXIN.getPubkey().getEncoded());
  }

  @Test
  public void getSigTest() {
    assertEquals(SIG, SCRIPTTXIN.getSig().getEncoded());
  }
}
