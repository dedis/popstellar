package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateKeyPair;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;

public class Input_objectTest {
  private static final int TX_OUT_INDEX = 0;
  private static final String Tx_OUT_HASH = "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=";

  private static final KeyPair SENDER_KEY = generateKeyPair();
  private static final PublicKey SENDER = SENDER_KEY.getPublicKey();

  private static String TYPE;
  private static String PUBKEY;
  private static String SIG;
  private static Script_input_object SCRIPTTXIN;
  private static Input_object INPUT;

  @Before
  public void setup() throws GeneralSecurityException {
    TYPE = "P2PKH";
    PUBKEY = SENDER.getEncoded();
    SIG = SENDER_KEY.sign(SENDER).getEncoded();
    SCRIPTTXIN = new Script_input_object(TYPE, PUBKEY, SIG);
    INPUT = new Input_object(Tx_OUT_HASH, TX_OUT_INDEX, SCRIPTTXIN);
  }

  @Test
  public void get_tx_out_indexTest() {
    assertEquals(TX_OUT_INDEX, INPUT.get_tx_out_index());
  }

  @Test
  public void get_tx_out_hashTest() {
    assertEquals(Tx_OUT_HASH, INPUT.get_tx_out_hash());
  }

  @Test
  public void get_scriptTest() {
    assertEquals(INPUT.get_script().get_pubkey(), PUBKEY);
    assertEquals(INPUT.get_script().get_sig(), SIG);
    assertEquals(INPUT.get_script().get_type(), TYPE);
  }
}
