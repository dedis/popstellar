package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class WitnessMessageTest {
  private static final PublicKey PK = Base64DataUtils.generatePublicKey();
  private static final MessageID MSG_ID = new MessageID("foo");
  private static final String DESCRIPTION = "description";
  private static final String TITLE = "title";
  private static final MessageID MESSAGE_ID1 = Base64DataUtils.generateMessageID();
  private static final MessageID MESSAGE_ID3 = Base64DataUtils.generateMessageID();

  private static final WitnessMessage MESSAGE1 = new WitnessMessage(MESSAGE_ID1);
  private static final WitnessMessage MESSAGE2 = MESSAGE1.copy();
  private static final WitnessMessage MESSAGE3 = new WitnessMessage(MESSAGE_ID3);

  @Test
  public void addWitnessTest() {
    WitnessMessage witnessMessage = new WitnessMessage(MSG_ID);
    witnessMessage.addWitness(PK);
    assertTrue(witnessMessage.getWitnesses().contains(PK));
  }

  @Test
  public void toStringTest() {
    WitnessMessage witnessMessage = new WitnessMessage(MSG_ID);
    witnessMessage.addWitness(PK);
    witnessMessage.setDescription(DESCRIPTION);
    witnessMessage.setTitle(TITLE);
    String expected =
        String.format(
            "WitnessMessage{messageId='%s', witnesses=%s, title='%s', description='%s'}",
            MSG_ID, Collections.singletonList(PK), TITLE, DESCRIPTION);
    assertEquals(expected, witnessMessage.toString());
  }

  @Test
  public void equalsTest() {
    assertEquals(MESSAGE1, MESSAGE2);
    assertNotEquals(MESSAGE1, MESSAGE3);

    WitnessMessage message1 = new WitnessMessage(MESSAGE1);
    message1.addWitness(PK);
    message1.setDescription(DESCRIPTION);
    message1.setTitle(TITLE);

    WitnessMessage message2 = new WitnessMessage(message1);
    assertEquals(message1, message2);

    message2.addWitness(Base64DataUtils.generatePublicKey());
    assertNotEquals(message1, message2);

    message2 = new WitnessMessage(message1);
    message2.setDescription("new description");
    assertNotEquals(message1, message2);

    message2 = new WitnessMessage(message1);
    message2.setTitle("new title");
    assertNotEquals(message1, message2);

    assertNotEquals(message1, null);
  }

  @Test
  public void hashCodeTest() {
    assertEquals(MESSAGE1.hashCode(), MESSAGE2.hashCode());
    assertNotEquals(MESSAGE2.hashCode(), MESSAGE3.hashCode());
  }
}
