package com.github.dedis.student20_pop.model;


import com.github.dedis.student20_pop.model.network.method.message.QuestionResult;
import com.github.dedis.student20_pop.utility.security.Hash;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ElectionTest {

    private String name = "my election name";
    private String id = "my election id";
    private String question = "my question";
    private boolean writeIn = false;
    private long startTime = 0;
    private long endTime = 1;
    private long creationTime = 0;
    private List<String> ballotOptions = Arrays.asList("candidate1", "candidate2");
    private Election election = new Election();

    @Test
    public void settingNullParametersThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> election.setName(null));
        assertThrows(IllegalArgumentException.class, () -> election.setBallotOptions(null));
        assertThrows(IllegalArgumentException.class, () -> election.setQuestion(null));
        assertThrows(IllegalArgumentException.class, () -> election.setId(null));
    }

    @Test
    public void settingBallotOptionsWithSizeLessThan2ThrowsException() {
        List<String> brokenBallotOptions = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> election.setBallotOptions(brokenBallotOptions));
        brokenBallotOptions.add("candidate1");
        assertThrows(IllegalArgumentException.class, () -> election.setBallotOptions(brokenBallotOptions));
    }

    @Test
    public void settingAndGettingReturnsCorrespondingName() {
        election.setName(name);
        assertThat(election.getName(), is(name));
    }

    @Test
    public void settingAndGettingReturnsCorrespondingId() {
        election.setId(id);
        assertThat(election.getId(), is(id));
    }

    @Test
    public void settingAndGettingReturnsCorrespondingQuestion() {
        election.setQuestion(question);
        assertThat(election.getQuestion(), is(question));
    }

    @Test
    public void settingAndGettingReturnsCorrespondingBallotOptions() {
        election.setBallotOptions(ballotOptions);
        assertThat(election.getBallotOptions(), is(ballotOptions));
    }

    @Test
    public void settingAndGettingReturnsCorrespondingWriteIn() {
        election.setWriteIn(writeIn);
        assertThat(election.getWriteIn(), is(writeIn));
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
    public void settingAndGettingReturnsCorrespondingCreationTime() {
        election.setStart(creationTime);
        assertThat(election.getCreation(), is(creationTime));
    }

    @Test
    public void settingSameRegisteredVotesAndComparingReturnsTrue() {
        List<String> registeredVotes = Arrays.asList("voteId1", "voteId2", "voteId3", "voteId4");
        String hashed = Hash.hash(registeredVotes.toString());
        election.setOrganizerRegisteredVotes(hashed);
        election.setWitnessRegisteredVotes(registeredVotes.toString());
        assertThat(election.compareRegisteredVotes(), is(true));
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
