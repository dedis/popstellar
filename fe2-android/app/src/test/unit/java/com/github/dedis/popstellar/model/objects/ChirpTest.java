package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.*;

public class ChirpTest {

  // By definition, a chirp having no parent has an empty message id as parent
  private static final MessageID ID = generateMessageID();
  private static final PublicKey SENDER = generatePublicKey();
  private static final String TEXT = "This is a Chirp !";
  private static final long TIMESTAMP = 10000;
  private static final MessageID EMPTY_MESSAGE_ID = new MessageID("");

  @Test
  public void createChirpWithEmptyIdFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Chirp(EMPTY_MESSAGE_ID, SENDER, TEXT, TIMESTAMP, EMPTY_MESSAGE_ID));
  }

  @Test
  public void createChirpWithNegativeTimestampFails() {
    assertThrows(
        IllegalArgumentException.class, () -> new Chirp(ID, SENDER, TEXT, -5, EMPTY_MESSAGE_ID));
  }

  @Test
  public void createChirpWithTooManyCharactersFails() {
    String textTooLong =
        "This text should be way over three hundred characters which is the current limit of the"
            + " text within a chirp, and if I try to set the chirp's text to this, it  should"
            + " throw and IllegalArgumentException() so I hope it does otherwise I might have"
            + " screwed something up. But normally it is not that hard to write enough to reach"
            + " the threshold.";

    assertThrows(
        IllegalArgumentException.class,
        () -> new Chirp(ID, SENDER, textTooLong, TIMESTAMP, EMPTY_MESSAGE_ID));
  }

  @Test
  public void deletedChirpProducesASimilarChirpWithEmptyTextAndDeletedProperty() {
    Chirp chirp = new Chirp(ID, SENDER, TEXT, TIMESTAMP, EMPTY_MESSAGE_ID);
    Chirp deleted = chirp.deleted();

    assertEquals(chirp.getId(), deleted.getId());
    assertEquals(chirp.getSender(), deleted.getSender());
    assertEquals("", deleted.getText());
    assertEquals(chirp.getTimestamp(), deleted.getTimestamp());
    assertEquals(chirp.getParentId(), deleted.getParentId());
    assertTrue(deleted.isDeleted());
  }
}
