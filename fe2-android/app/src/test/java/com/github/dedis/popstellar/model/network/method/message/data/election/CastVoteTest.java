package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.dedis.popstellar.di.DataRegistryModule;
import com.github.dedis.popstellar.di.JsonModule;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.serializer.JsonUtils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CastVoteTest {

  private final String questionId1 = " myQuestion1";
  private final String questionId2 = " myQuestion2";
  private final String laoId = "myLao";
  private final String electionId = " myElection";
  private final boolean writeInEnabled = false;
  private final String write_in = "My write in ballot option";
  private static final Gson GSON = JsonModule.provideGson(DataRegistryModule.provideDataRegistry());

  // Set up a open ballot election
  private final ElectionVote electionVote1 =
          new ElectionVote(questionId1, 1, writeInEnabled, write_in, electionId);
    private final ElectionVote electionVote2 =
            new ElectionVote(questionId2, 2, writeInEnabled, write_in, electionId);
    private final List<ElectionVote> electionVotes = Arrays.asList(electionVote1, electionVote2);

  // Set up a secret ballot election
  private final ElectionEncryptedVote electionEncryptedVote1 =
          new ElectionEncryptedVote(questionId1, "2", writeInEnabled, write_in, electionId);
  private final ElectionEncryptedVote electionEncryptedVote2 =
          new ElectionEncryptedVote(questionId2, "1", writeInEnabled, write_in, electionId);
  private final List<ElectionEncryptedVote> electionEncryptedVotes = Arrays.asList(electionEncryptedVote1, electionEncryptedVote2);

  // Create the cast votes messages
  private final CastVote<ElectionVote> castOpenVote = new CastVote(electionVotes,  electionId, laoId);
  private final CastVote<ElectionEncryptedVote> castEncryptedVote = new CastVote(electionEncryptedVotes, electionId, laoId);

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
    assertThat(electionVotes, is(castOpenVote.getVotes()));
    assertThat(electionEncryptedVotes, is(castEncryptedVote.getVotes()));
  }

  @Test
  public void isEqualTest() {
    // Test an OPEN_BALLOT cast vote
    assertEquals(castOpenVote, new CastVote(electionVotes,electionId, laoId));
    assertEquals(castOpenVote, castOpenVote);
    assertNotEquals(
        castOpenVote, new CastVote(Collections.singletonList(electionVote1), electionId, laoId));
    assertNotEquals(
            castOpenVote, new CastVote(Collections.singletonList(electionVote1),"random", laoId));
    assertNotEquals(
             castOpenVote, new CastVote(Collections.singletonList(electionVote1),electionId, "random"));

    // Test a SECRET_BALLOT cast vote
    assertEquals(castEncryptedVote, new CastVote(electionEncryptedVotes, electionId, laoId));
    assertEquals(castEncryptedVote, castEncryptedVote);
    assertNotEquals(
            castEncryptedVote, new CastVote(Collections.singletonList(electionEncryptedVote1), electionId, laoId));
    assertNotEquals(
            castEncryptedVote, new CastVote(Collections.singletonList(electionEncryptedVote1), "random", laoId));
    assertNotEquals(
            castEncryptedVote, new CastVote(Collections.singletonList(electionEncryptedVote1), electionId, "random"));
  }

  /**
   * Deserialization needs a specific generic type to match correctly the class
   */
  @Test
  public void jsonValidationTestOpenBallot() {
    // Schema should be valid with both vote lists
    // For testing now, create a custom JSON Test parser
    String json = GSON.toJson(castOpenVote, Data.class);
    JsonUtils.verifyJson(JsonUtils.DATA_SCHEMA, json);
    Type token = new TypeToken<CastVote<ElectionVote>>() {
    }.getType();
    assertEquals(castOpenVote, GSON.fromJson(json, token));
  }

  /**
   * Deserialization needs a specific generic type to match correctly the class
   */
  @Test
  public void jsonValidationTestSecretBallot() {
    // Schema should be valid with both vote lists
    // For testing now, create a custom JSON Test parser
    String json = GSON.toJson(castEncryptedVote, Data.class);
    JsonUtils.verifyJson(JsonUtils.DATA_SCHEMA, json);
    Type token = new TypeToken<CastVote<ElectionEncryptedVote>>() {
    }.getType();
    assertEquals(castEncryptedVote, GSON.fromJson(json, token));
  }

}
