package com.github.dedis.popstellar.model.objects;

import static com.github.dedis.popstellar.model.objects.event.EventState.OPENED;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionResultQuestion;
import com.github.dedis.popstellar.model.network.method.message.data.election.ElectionVote;
import com.github.dedis.popstellar.model.network.method.message.data.election.QuestionResult;
import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElectionTest {

  private ElectionQuestion electionQuestion =
      new ElectionQuestion(
          "my question",
          "Plurality",
          false,
          Arrays.asList("candidate1", "candidate2"),
          "my election id");
  private String name = "my election name";
  private String id = "my election id";
  private long startTime = 0;
  private long endTime = 1;
  private String channel = "channel id";
  private Election election = new Election("lao id", Instant.now().getEpochSecond(), name);

  @Test
  public void settingNullParametersThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setName(null));
    assertThrows(IllegalArgumentException.class, () -> election.setId(null));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingName() {
    election.setName(name);
    assertThat(election.getName(), is(name));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingState() {
    election.setEventState(OPENED);
    assertThat(election.getState(), is(OPENED));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingId() {
    election.setId(id);
    assertThat(election.getId(), is(id));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingElectionQuestion() {
    election.setElectionQuestions(Arrays.asList(electionQuestion));
    assertThat(election.getElectionQuestions().get(0), is(electionQuestion));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingChannel() {
    election.setChannel(channel);
    assertThat(election.getChannel(), is(channel));
  }

  @Test
  public void settingNegativeTimesThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setStart(-1));
    assertThrows(IllegalArgumentException.class, () -> election.setEnd(-1));
    assertThrows(IllegalArgumentException.class, () -> election.setCreation(-1));
  }

  @Test
  public void settingNullElectionQuestionsThrowsException() {
    assertThrows(IllegalArgumentException.class, () -> election.setElectionQuestions(null));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingStartTime() {
    election.setStart(startTime);
    assertThat(election.getStartTimestamp(), is(startTime));
  }

  @Test
  public void settingAndGettingReturnsCorrespondingEndTime() {
    election.setEnd(endTime);
    assertThat(election.getEndTimestamp(), is(endTime));
  }

  @Test
  public void settingSameRegisteredVotesAndComparingReturnsTrue() {
    List<ElectionVote> votes1 =
        Arrays.asList(
            new ElectionVote("b", Arrays.asList(1), false, "", "my election id"),
            new ElectionVote("a", Arrays.asList(2), false, "", "my election id"));
    List<ElectionVote> votes2 =
        Arrays.asList(
            new ElectionVote("c", Arrays.asList(3), false, "", "my election id"),
            new ElectionVote("d", Arrays.asList(4), false, "", "my election id"));
    election.putVotesBySender("sender2", votes2);
    election.putSenderByMessageId("sender1", "message1");
    election.putSenderByMessageId("sender2", "message2");
    election.putVotesBySender("sender1", votes1);
    String hash =
        Hash.hash(
            votes1.get(1).getId(),
            votes1.get(0).getId(),
            votes2.get(0).getId(),
            votes2.get(1).getId());
    assertThat(election.computerRegisteredVotes(), is(hash));
  }

  @Test
  public void resultsAreCorrectlySorted() {
    List<QuestionResult> unsortedResults = new ArrayList<>();
    unsortedResults.add(new QuestionResult("Candidate1", 30));
    unsortedResults.add(new QuestionResult("Candidate2", 23));
    unsortedResults.add(new QuestionResult("Candidate3", 16));
    unsortedResults.add(new QuestionResult("Candidate4", 43));
    List<ElectionResultQuestion> resultQuestion =
        Arrays.asList(new ElectionResultQuestion("question_id", unsortedResults));
    election.setResults(resultQuestion);
    List<QuestionResult> sortedResults = election.getResultsForQuestionId("question_id");

    QuestionResult firstResult = sortedResults.get(0);
    assertThat(firstResult.getBallot(), is("Candidate4"));
    assertThat(firstResult.getCount(), is(43));

    QuestionResult secondResult = sortedResults.get(1);
    assertThat(secondResult.getBallot(), is("Candidate1"));
    assertThat(secondResult.getCount(), is(30));

    QuestionResult thirdResult = sortedResults.get(2);
    assertThat(thirdResult.getBallot(), is("Candidate2"));
    assertThat(thirdResult.getCount(), is(23));

    QuestionResult fourthResult = sortedResults.get(3);
    assertThat(fourthResult.getBallot(), is("Candidate3"));
    assertThat(fourthResult.getCount(), is(16));
  }
}
