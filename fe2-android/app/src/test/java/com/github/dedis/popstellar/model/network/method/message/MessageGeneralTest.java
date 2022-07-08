package com.github.dedis.popstellar.model.network.method.message;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.lao.CreateLao;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.Base64URLData;
import com.github.dedis.popstellar.model.objects.security.KeyPair;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.security.Signature;
import com.github.dedis.popstellar.model.objects.security.privatekey.PlainPrivateKey;
import com.google.gson.Gson;

import net.i2p.crypto.eddsa.Utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageGeneralTest {

  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());

  private static final PublicKey ORGANIZER =
      new PublicKey("Z3DYtBxooGs6KxOAqCWD3ihR8M6ZPBjAmWp_w5VBaws=");
  private static final long LAO_CREATION = 1623825071;
  private static final String LAO_NAME = "LAO";

  private static final CreateLao DATA =
      new CreateLao(
          Lao.generateLaoId(ORGANIZER, LAO_CREATION, LAO_NAME),
          LAO_NAME,
          LAO_CREATION,
          ORGANIZER,
          new ArrayList<>());
  private static final Base64URLData DATA_ENCODED =
      new Base64URLData(
          "eyJpZCI6Ik5PZjlHTGZKWTVjVVJkaUptaWxZcnNZT1phay1rXzd2MnV6NGxsQ1NFMU09IiwibmFtZSI6IkxBTyIsImNyZWF0aW9uIjoxNjIzODI1MDcxLCJvcmdhbml6ZXIiOiJaM0RZdEJ4b29HczZLeE9BcUNXRDNpaFI4TTZaUEJqQW1XcF93NVZCYXdzPSIsIndpdG5lc3NlcyI6W10sIm9iamVjdCI6ImxhbyIsImFjdGlvbiI6ImNyZWF0ZSJ9");
  private static final Signature SIGNATURE =
      new Signature(
          "OWT7z5-L25kCwFKvA0Rdz0HVXV57I8WZo183-skoEqfbojNLA78SEYhZjW6hT1lGJFGU2HefTkMBQzS49OkCDg==");
  private static final MessageID MESSAGE_ID =
      new MessageID("3ZIQn9IRUQSBQChkb6gxRj_iYjXAiO-nx1KSBM6b79M=");

  private static final KeyPair KEY_PAIR =
      new KeyPair(
          new PlainPrivateKey(
              Utils.hexToBytes("3b28b4ab2fe355a13d7b24f90816ff0676f7978bf462fc84f1d5d948b119ec66")),
          new PublicKey("5c2zk_5uCrrNmdUhQAloCDqYJAC2rD4KHo9gGNFVS9c="));

  private static final List<PublicKeySignaturePair> WITNESS_SIGNATURES =
      Collections.singletonList(
          new PublicKeySignaturePair(
              new PublicKey("FOosAAfgtHv0g_qZ5MyYTuvyiNXWtvrb0dMF4LY5O8M="),
              new Signature(
                  "7vcWfBmA0vU9_dkRxukAfMiWJRJOrERqLrIqFIpGZItTMSS0ZncurPamzd4seXZMR25yV9HIcyYeRJDZz4rUCGV5SnBaQ0k2SWs1UFpqbEhUR1pLV1RWalZWSmthVXB0YVd4WmNuTlpUMXBoYXkxclh6ZDJNblY2Tkd4c1ExTkZNVTA5SWl3aWJtRnRaU0k2SWt4QlR5SXNJbU55WldGMGFXOXVJam94TmpJek9ESTFNRGN4TENKdmNtZGhibWw2WlhJaU9pSmFNMFJaZEVKNGIyOUhjelpMZUU5QmNVTlhSRE5wYUZJNFRUWmFVRUpxUVcxWGNGOTNOVlpDWVhkelBTSXNJbmRwZEc1bGMzTmxjeUk2VzEwc0ltOWlhbVZqZENJNklteGhieUlzSW1GamRHbHZiaUk2SW1OeVpXRjBaU0o5")));

  @Test
  public void testConstructorWithData() {
    MessageGeneral msg = new MessageGeneral(KEY_PAIR, DATA, GSON);

    assertThat(msg.getData(), is(DATA));
    assertThat(msg.getSender(), is(KEY_PAIR.getPublicKey()));
    assertThat(msg.getWitnessSignatures(), is(Collections.emptyList()));
  }

  @Test
  public void testConstructorWithDataAndWitnessSignatures() {
    MessageGeneral msg = new MessageGeneral(KEY_PAIR, DATA, WITNESS_SIGNATURES, GSON);

    assertThat(msg.getData(), is(DATA));
    assertThat(msg.getSender(), is(KEY_PAIR.getPublicKey()));
    assertThat(msg.getWitnessSignatures(), is(WITNESS_SIGNATURES));
  }

  @Test
  public void testValueGeneration() throws GeneralSecurityException {
    MessageGeneral msg = new MessageGeneral(KEY_PAIR, DATA, GSON);

    assertThat(
        msg.getDataEncoded(),
        is(new Base64URLData(GSON.toJson(DATA, Data.class).getBytes(StandardCharsets.UTF_8))));
    assertThat(msg.getSignature(), is(KEY_PAIR.getPrivateKey().sign(msg.getDataEncoded())));
    assertThat(msg.getMessageId(), is(new MessageID(msg.getDataEncoded(), msg.getSignature())));
  }

  @Test
  public void testFixedValueGeneration() {
    MessageGeneral msg = new MessageGeneral(KEY_PAIR, DATA, GSON);

    assertThat(msg.getDataEncoded(), is(DATA_ENCODED));
    assertThat(msg.getSignature(), is(SIGNATURE));
    assertThat(msg.getMessageId(), is(MESSAGE_ID));
  }

  @Test
  public void verifyWorksOnValidData() {
    MessageGeneral msg1 = new MessageGeneral(KEY_PAIR, DATA, WITNESS_SIGNATURES, GSON);
    MessageGeneral msg2 =
        new MessageGeneral(
            KEY_PAIR.getPublicKey(), DATA_ENCODED, DATA, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES);

    assertThat(msg1.verify(), is(true));
    assertThat(msg2.verify(), is(true));
  }

  @Test
  public void verifyFailsOnInvalidData() {
    MessageGeneral msg =
        new MessageGeneral(
            KEY_PAIR.getPublicKey(),
            DATA_ENCODED,
            DATA,
            new Signature("UB6xpjpUGN5VtmWAw1T3npHxiZfKaXzx3ny5PXl_qF4"),
            MESSAGE_ID,
            WITNESS_SIGNATURES);

    assertThat(msg.verify(), is(false));
  }

  @Test
  public void toStringTest() {
    MessageGeneral msg =
        new MessageGeneral(
            KEY_PAIR.getPublicKey(), DATA_ENCODED, DATA, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES);
    System.out.println(msg);
    String expected =
        String.format(
            "MessageGeneral{sender='%s', data='%s', signature='%s', messageId='%s', "
                + "witnessSignatures='%s'}",
            KEY_PAIR.getPublicKey().toString(), DATA, SIGNATURE, MESSAGE_ID, WITNESS_SIGNATURES);
    assertEquals(expected, msg.toString());
  }
}
