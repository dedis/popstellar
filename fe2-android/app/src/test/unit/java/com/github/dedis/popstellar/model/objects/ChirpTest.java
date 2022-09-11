package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;

import org.junit.Test;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.generateMessageID;
import static com.github.dedis.popstellar.testutils.Base64DataUtils.generatePublicKey;
import static org.junit.Assert.*;

public class ChirpTest {

  // By definition, a chirp having no parent has an empty message id as parent
  private static final MessageID EMPTY_MESSAGE_ID = new MessageID("");
  private static final Chirp CHIRP =
      new Chirp(
          generateMessageID(), generatePublicKey(), "This is a chirp !", 10000, EMPTY_MESSAGE_ID);

  @Test
  public void createChirpWithEmptyIdFails() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Chirp(
                EMPTY_MESSAGE_ID,
                CHIRP.getSender(),
                CHIRP.getText(),
                CHIRP.getTimestamp(),
                CHIRP.getParentId()));
  }

  @Test
  public void createChirpWithNegativeTimestampFails() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Chirp(CHIRP.getId(), CHIRP.getSender(), CHIRP.getText(), -5, CHIRP.getParentId()));
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
        () ->
            new Chirp(
                CHIRP.getId(),
                CHIRP.getSender(),
                textTooLong,
                CHIRP.getTimestamp(),
                CHIRP.getParentId()));
  }

  @Test
  public void deletedChirpProducesASimilarChirpWithEmptyTextAndDeletedProperty() {
    Chirp deleted = CHIRP.deleted();

    assertEquals(CHIRP.getId(), deleted.getId());
    assertEquals(CHIRP.getSender(), deleted.getSender());
    assertEquals("", deleted.getText());
    assertEquals(CHIRP.getTimestamp(), deleted.getTimestamp());
    assertEquals(CHIRP.getParentId(), deleted.getParentId());
    assertTrue(deleted.isDeleted());
  }
}
