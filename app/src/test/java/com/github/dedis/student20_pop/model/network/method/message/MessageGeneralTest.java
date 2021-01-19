package com.github.dedis.student20_pop.model.network.method.message;

import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.network.method.message.MessageGeneral;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.utility.security.Signature;
import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class MessageGeneralTest {

  private final Keys sender1 = new Keys();
  private final Keys sender2 = new Keys();
  private final String data = "data";
  private final String signature1 = Signature.sign(sender1.getPrivateKey(), data);
  private final String signature2 = Signature.sign(sender2.getPrivateKey(), data);
  private final String messageId1 = Hash.hash(data, signature1);
  private final String messageId2 = Hash.hash(data, signature2);
  private final MessageGeneral messageGeneral1 =
      new MessageGeneral(sender1.getPublicKey(), data, signature1, messageId1, new ArrayList<>());
  private final MessageGeneral messageGeneral2 =
      new MessageGeneral(sender2.getPublicKey(), data, signature2, messageId2, new ArrayList<>());

  @Test
  public void createMessageGeneralWithNullParametersTest() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new MessageGeneral(null, data, signature1, messageId1, new ArrayList<>()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MessageGeneral(
                sender1.getPublicKey(), null, signature1, messageId1, new ArrayList<>()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MessageGeneral(sender1.getPublicKey(), data, null, messageId1, new ArrayList<>()));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new MessageGeneral(sender1.getPublicKey(), data, signature1, null, new ArrayList<>()));
    assertThrows(
        IllegalArgumentException.class,
        () -> new MessageGeneral(sender1.getPublicKey(), data, signature1, messageId1, null));
  }

  @Test
  public void getSenderTest() {
    assertThat(messageGeneral1.getSender(), is(sender1.getPublicKey()));
  }

  @Test
  public void getDataTest() {
    assertThat(messageGeneral1.getData(), is(data));
  }

  @Test
  public void getSignatureTest() {
    assertThat(messageGeneral1.getSignature(), is(signature1));
  }

  @Test
  public void getMessageIdTest() {
    assertThat(messageGeneral1.getMessageId(), is(messageId1));
  }

  @Test
  public void getWitnessSignatures() {
    assertThat(messageGeneral1.getWitnessSignatures(), is(new ArrayList<>()));
  }

  @Test
  public void equalsTest() {
    assertEquals(messageGeneral1, messageGeneral1);
    assertNotEquals(messageGeneral2, messageGeneral1);
  }

  @Test
  public void hashCodeTest() {
    assertEquals(messageGeneral1.hashCode(), messageGeneral1.hashCode());
    assertNotEquals(messageGeneral1.hashCode(), messageGeneral2.hashCode());
  }
}
