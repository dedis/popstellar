package com.github.dedis.popstellar.model.objects;

import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;

import org.junit.Test;

import java.time.Instant;

import static com.github.dedis.popstellar.testutils.Base64DataUtils.*;
import static org.junit.Assert.*;

public class ReactionTest {

  private static final MessageID REACTION_ID = generateMessageID();
  private static final MessageID CHIRP_ID = generateMessageIDOtherThan(REACTION_ID);
  private static final PublicKey SENDER = generatePublicKey();
  private static final String EMOJI = "\uD83D\uDC4D";
  private static final long TIMESTAMP = Instant.now().getEpochSecond();
  private static final MessageID EMPTY_MESSAGE_ID = generateMessageID();
  private static final Reaction REACTION =
      new Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, TIMESTAMP);

  @Test
  public void createReactionWithEmptyIdFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Reaction(EMPTY_MESSAGE_ID, SENDER, EMOJI, CHIRP_ID, TIMESTAMP));
  }

  @Test
  public void createReactionWithEmptyChirpIdFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Reaction(REACTION_ID, SENDER, EMOJI, EMPTY_MESSAGE_ID, TIMESTAMP));
  }

  @Test
  public void createReactionWithFutureTimestampFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, TIMESTAMP + 100000));
  }

  @Test
  public void createReactionWithPastTimestampFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Reaction(REACTION_ID, SENDER, EMOJI, CHIRP_ID, -1));
  }

  @Test
  public void createReactionWithWrongEmojiChirpIdFails() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new Reaction(REACTION_ID, SENDER, "\uD93D\uDC4D", CHIRP_ID, TIMESTAMP));
  }

  @Test
  public void createDeletedReaction() {
    Reaction deleted = REACTION.deleted();
    assertEquals(REACTION.getId(), deleted.getId());
    assertEquals(REACTION.getSender(), deleted.getSender());
    assertEquals(REACTION.getChirpId(), deleted.getChirpId());
    assertEquals(REACTION.getTimestamp(), deleted.getTimestamp());
    assertEquals(REACTION.getCodepoint(), deleted.getCodepoint());
    assertTrue(deleted.isDeleted());
  }
}
