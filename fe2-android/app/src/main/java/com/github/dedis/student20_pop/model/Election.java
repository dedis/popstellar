package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Election extends Event {

    private String id;
    private String name;
    private long creation;
    private long start;
    private long end;
    private boolean writeIn;
    private String question;
    private List<String> ballotOptions;

    //Used by witness to store in local the hash corresponding to the registered votes
    private String witnessRegisteredVotes;
    //Registered votes recorded by the organizer at the end of an election
    private String organizerRegisteredVotes;

    private Map<String, Integer> resultsMap;

    public Election() {
        this.ballotOptions = new ArrayList<>();
        this.resultsMap = new LinkedHashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null) throw new IllegalArgumentException("Election's id shouldn't be null");
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) throw new IllegalArgumentException("Election's name shouldn't be null");
        this.name = name;
    }

    public long getCreation() {
        return creation;
    }


    private void checkTime(long time) {
        if (time < 0) throw new IllegalArgumentException("A time can't be negative");
    }

    public void setCreation(long creation) {
        checkTime(creation);
        this.creation = creation;
    }

    public void setStart(long start) {
        checkTime(start);
        this.start = start;
    }

    public void setEnd(long end) {
        checkTime(end);
        this.end = end;
    }

    /**
     * Takes the votes ids, and hashes them into a string. It can then be used by the witness to compare with
     * the registered votes sent by the organizer at the end of an election.
     * @param registeredVoteIds
     */
    public void setWitnessRegisteredVotes(String... registeredVoteIds) {
        if (registeredVoteIds == null) throw new IllegalArgumentException("registered votes shouldn't be null");
        this.witnessRegisteredVotes = Hash.hash(registeredVoteIds);
    }

    public void setOrganizerRegisteredVotes(String organizerRegisteredVotes) {
        if (organizerRegisteredVotes == null || organizerRegisteredVotes.isEmpty()) throw new IllegalArgumentException("registered votes shouldn't be null, nor empty.");
        this.organizerRegisteredVotes = organizerRegisteredVotes;
    }


    public void setResultsMap(Map<String, Integer> unsortedWinnerMap) {
        if (unsortedWinnerMap == null) throw new IllegalArgumentException("the map of winners shoud not be null");
        //Sorts the map in descending order of votes
        unsortedWinnerMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> resultsMap.put(x.getKey(), x.getValue()));
    }

    public Map<String, Integer> getResultsMap() {
        return resultsMap;
    }

    public List<String> getBallotOptions() {
        return ballotOptions;
    }

    public void setBallotOptions(List<String> ballotOptions) {
        if (ballotOptions == null)
            throw new IllegalArgumentException("ballot options can't be null");
        if (ballotOptions.size() < 2)
            throw new IllegalArgumentException("ballot must have at least two options");
        this.ballotOptions = ballotOptions;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        if (question == null) throw new IllegalArgumentException("question can't be null");
        this.question = question;
    }

    public boolean getWriteIn() {
        return writeIn;
    }

    public void setWriteIn(boolean writeIn) {
        this.writeIn = writeIn;
    }

    @Override
    public long getStartTimestamp() {
        return start;
    }

    @Override
    public long getEndTimestamp() {
        return end;
    }

    /**
     * This method is used by the witness to compare local registered votes with
     * the ones received from organizer back-end
     */
    public boolean compareRegisteredVotes() {
        if (witnessRegisteredVotes == null) throw new IllegalArgumentException("Witness registered votes have not been set !");
        if (organizerRegisteredVotes == null) throw new IllegalArgumentException("Organizer registered votes have not been set !");
        return witnessRegisteredVotes.equals(organizerRegisteredVotes);
    }
}
