package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WitnessMessageTest {
  private static final PublicKey PK = Base64DataUtils.generatePublicKey();
  private static final MessageID MSG_ID = new MessageID("foo");
  private static final String DESCRIPTION = "description";
  private static final String TITLE = "title";

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
}
