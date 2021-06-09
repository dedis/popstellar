package com.github.dedis.student20_pop.model;


import com.github.dedis.student20_pop.model.network.method.message.ElectionQuestion;
import com.github.dedis.student20_pop.model.network.method.message.ElectionVote;
import com.github.dedis.student20_pop.model.network.method.message.QuestionResult;
import com.github.dedis.student20_pop.utility.security.Hash;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ElectionTest {

    private ElectionQuestion electionQuestion = new ElectionQuestion("my question", "Plurality",
            false, Arrays.asList("candidate1", "candidate2"), "my election id");
    private String name = "my election name";
    private String id = "my election id";
    private long startTime = 0;
    private long endTime = 1;
    private String channel = "channel id";
    private Election election = new Election();

    @Test
    public void settingAndGettingReturnsCorrespondingName() {
        election.setName(name);
        assertThat(election.getName(), is(name));
    }

    @Test
    public void settingAndGettingReturnsCorrespondingEndBoolean() {
        assertThat(election.isEnded(), is(false));
        election.setEnded(true);
        assertThat(election.isEnded(), is(true));
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
        List<ElectionVote> votes1 = Arrays.asList(new ElectionVote("b", Arrays.asList(1), false, "", "my election id"),
                new ElectionVote("a", Arrays.asList(2), false, "", "my election id"));
        List<ElectionVote> votes2 = Arrays.asList(new ElectionVote("c", Arrays.asList(3), false, "", "my election id"),
                new ElectionVote("d", Arrays.asList(4), false, "", "my election id"));
        election.putVotesBySender("sender2", votes2);
        election.putSenderByMessageId("sender1", "message1");
        election.putSenderByMessageId("sender2", "message2");
        election.putVotesBySender("sender1", votes1);
        String hash = Hash.hash(votes1.get(1).getId(), votes1.get(0).getId(), votes2.get(0).getId(), votes2.get(1).getId());
        assertThat(election.computerRegisteredVotes(), is(hash));
    }
    
    @Test
    public void resultsAreCorrectlySorted() {
        List<QuestionResult> unsortedResults = new ArrayList<>();
        unsortedResults.add(new QuestionResult("Candidate1", 30));
        unsortedResults.add(new QuestionResult("Candidate2", 23));
        unsortedResults.add(new QuestionResult("Candidate3", 16));
        unsortedResults.add(new QuestionResult("Candidate4", 43));
        election.setResults(unsortedResults);
        List<QuestionResult> sortedResults = election.getResults();

        QuestionResult firstResult = sortedResults.get(0);
        assertThat(firstResult.getName(), is("Candidate4"));
        assertThat(firstResult.getCount(), is(43));

        QuestionResult secondResult = sortedResults.get(1);
        assertThat(secondResult.getName(), is("Candidate1"));
        assertThat(secondResult.getCount(), is(30));

        QuestionResult thirdResult = sortedResults.get(2);
        assertThat(thirdResult.getName(), is("Candidate2"));
        assertThat(thirdResult.getCount(), is(23));

        QuestionResult fourthResult = sortedResults.get(3);
        assertThat(fourthResult.getName(), is("Candidate3"));
        assertThat(fourthResult.getCount(), is(16));

    }
}
