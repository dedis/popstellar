package com.github.dedis.student20_pop.model;


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
    public void resultsAreCorrectlySorted() {
        Map<String, Integer> unsortedResultsMap = new LinkedHashMap<>();
        unsortedResultsMap.put("Candidate1", 30);
        unsortedResultsMap.put("Candidate2", 23);
        unsortedResultsMap.put("Candidate3", 16);
        unsortedResultsMap.put("Candidate4", 43);
        election.setResultsMap(unsortedResultsMap);
        Map<String, Integer> sortedMap = election.getResultsMap();
        Iterator<Map.Entry<String, Integer>> mapIterator = sortedMap.entrySet().iterator();

        Map.Entry<String, Integer> firstResult = mapIterator.next();
        assertThat(firstResult.getKey(), is("Candidate4"));
        assertThat(firstResult.getValue(), is(43));

        Map.Entry<String, Integer> secondResult = mapIterator.next();
        assertThat(secondResult.getKey(), is("Candidate1"));
        assertThat(secondResult.getValue(), is(30));

        Map.Entry<String, Integer> thirdResult = mapIterator.next();
        assertThat(thirdResult.getKey(), is("Candidate2"));
        assertThat(thirdResult.getValue(), is(23));

        Map.Entry<String, Integer> fourthResult = mapIterator.next();
        assertThat(fourthResult.getKey(), is("Candidate3"));
        assertThat(fourthResult.getValue(), is(16));

    }
}
