package com.github.dedis.popstellar.model.network.method.message.data.election;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.dedis.popstellar.model.network.JsonUtilsTest;
import com.github.dedis.popstellar.model.objects.Election;
import com.github.dedis.popstellar.model.objects.Lao;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.testutils.Base64DataUtils;
import com.google.gson.JsonParseException;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Instant;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CastVoteTest {

  private final PublicKey organizer = Base64DataUtils.generatePublicKey();
  private final long creation = Instant.now().getEpochSecond();
  private final String laoId = Lao.generateLaoId(organizer, creation, "lao name");
  private final String electionId =
      Election.generateElectionSetupId(laoId, creation, "electionName");

  private final String questionId1 = Election.generateElectionQuestionId(electionId, "Question 1");
  private final String questionId2 = Election.generateElectionQuestionId(electionId, "Question 2");
  private final boolean writeInEnabled = false;
  private final String write_in = "My write in ballot option";

  // Set up a open ballot election
  private final PlainVote plainVote1 =
      new PlainVote(questionId1, 1, writeInEnabled, write_in, electionId);
  private final PlainVote plainVote2 =
      new PlainVote(questionId2, 2, writeInEnabled, write_in, electionId);
  private final List<Vote> plainVotes = Arrays.asList(plainVote1, plainVote2);

  // Set up a secret ballot election
  private final EncryptedVote encryptedVote1 =
      new EncryptedVote(questionId1, "2", writeInEnabled, write_in, electionId);
  private final EncryptedVote encryptedVote2 =
      new EncryptedVote(questionId2, "1", writeInEnabled, write_in, electionId);
  private final List<Vote> electionEncryptedVotes = Arrays.asList(encryptedVote1, encryptedVote2);

  // Create the cast votes messages
  private final CastVote castOpenVote = new CastVote(plainVotes, electionId, laoId);
  private final CastVote castVoteWithTimestamp =
      new CastVote(plainVotes, electionId, laoId, creation);
  private final CastVote castEncryptedVote =
      new CastVote(electionEncryptedVotes, electionId, laoId);

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsElectionIdNotBase64Test() {
    new CastVote(plainVotes, "not base 64", laoId, creation);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsLaoIdNotBase64Test() {
    new CastVote(plainVotes, electionId, "not base 64", creation);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsCreationTooOldTest() {
    new CastVote(plainVotes, electionId, laoId, 1L);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsCreationInFutureTest() {
    new CastVote(plainVotes, electionId, laoId, creation + 1000);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsWithDuplicateVotesTest() {
    PlainVote duplicate = new PlainVote(questionId1, 1, writeInEnabled, write_in, electionId);
    List<Vote> duplicateVotes = Arrays.asList(plainVote1, plainVote2, duplicate);

    new CastVote(duplicateVotes, electionId, laoId, creation);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorFailsWithVoteQuestionIdNotBase64Test() {
    PlainVote invalid = new PlainVote("not base 64", 1, writeInEnabled, write_in, electionId);
    List<Vote> invalidVotes = Arrays.asList(plainVote1, plainVote2, invalid);

    new CastVote(invalidVotes, electionId, laoId, creation);
  }

  @Test
  public void getLaoIdTest() {
    assertThat(castOpenVote.getLaoId(), is(laoId));
    assertThat(castEncryptedVote.getLaoId(), is(laoId));
  }

  @Test
  public void getElectionIdTest() {
    assertThat(castOpenVote.getElectionId(), is(electionId));
    assertThat(castEncryptedVote.getElectionId(), is(electionId));
  }

  @Test
  public void getVotesTest() {
    assertThat(plainVotes, is(castOpenVote.getVotes()));
    assertThat(electionEncryptedVotes, is(castEncryptedVote.getVotes()));
  }

  @Test
  public void isEqualTest() {
    // Test an OPEN_BALLOT cast vote
    assertEquals(castOpenVote, new CastVote(plainVotes, electionId, laoId));
    assertEquals(castOpenVote, castOpenVote);
    String randomId = Election.generateElectionSetupId(laoId, creation, "random");
    assertNotEquals(
        castOpenVote, new CastVote(Collections.singletonList(plainVote1), electionId, laoId));
    assertNotEquals(
        castOpenVote, new CastVote(Collections.singletonList(plainVote1), randomId, laoId));
    assertNotEquals(
        castOpenVote, new CastVote(Collections.singletonList(plainVote1), electionId, randomId));
    assertEquals(castVoteWithTimestamp, new CastVote(plainVotes, electionId, laoId, creation));

    // Test a SECRET_BALLOT cast vote
    assertEquals(castEncryptedVote, new CastVote(electionEncryptedVotes, electionId, laoId));
    assertEquals(castEncryptedVote, castEncryptedVote);
    assertNotEquals(
        castEncryptedVote,
        new CastVote(Collections.singletonList(encryptedVote1), electionId, laoId));
    assertNotEquals(
        castEncryptedVote,
        new CastVote(Collections.singletonList(encryptedVote1), randomId, laoId));
    assertNotEquals(
        castEncryptedVote,
        new CastVote(Collections.singletonList(encryptedVote1), electionId, randomId));
  }

  /** Deserialization needs a specific generic type to match correctly the class */
  @Test
  public void jsonValidationTest() {
    // Schema should be valid with both vote lists
    // Should use the custom deserializer
    JsonUtilsTest.testData(castEncryptedVote);
    JsonUtilsTest.testData(castOpenVote);

    String pathDir = "protocol/examples/messageData/vote_cast_vote/";
    String jsonValid1 = JsonUtilsTest.loadFile(pathDir + "vote_cast_vote.json");
    String jsonValid2 = JsonUtilsTest.loadFile(pathDir + "vote_cast_vote_encrypted.json");
    JsonUtilsTest.parse(jsonValid1);
    JsonUtilsTest.parse(jsonValid2);

    String jsonInvalid1 =
        JsonUtilsTest.loadFile(pathDir + "wrong_vote_cast_vote_created_at_negative.json");

    assertThrows(JsonParseException.class, () -> JsonUtilsTest.parse(jsonInvalid1));
  }
}
