package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;

public class Script_input_objectTest {

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static String TYPE;
  private static String PUBKEY;
  private static String SIG;
  private static Script_input_object SCRIPTTXIN;

  @Before
  public void setup() throws GeneralSecurityException {
    TYPE = "P2PKH";
    PUBKEY = SENDER.getEncoded();
    SIG = SENDER_KEY.sign(SENDER).getEncoded();
    SCRIPTTXIN = new Script_input_object(TYPE, PUBKEY, SIG);
  }

  @Test
  public void gettypetest() {
    assertEquals(TYPE, SCRIPTTXIN.get_type());
  }

  @Test
  public void getpubkeytest() {
    assertEquals(PUBKEY, SCRIPTTXIN.get_pubkey());
  }

  @Test
  public void getsigtest() {
    assertEquals(SIG, SCRIPTTXIN.get_sig());
  }
}
