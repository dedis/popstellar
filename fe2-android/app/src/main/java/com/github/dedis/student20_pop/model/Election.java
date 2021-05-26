package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.model.network.method.message.QuestionResult;
import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionResult;
import com.github.dedis.student20_pop.utility.security.Hash;
import com.github.dedis.student20_pop.model.event.EventType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

    private List<QuestionResult> results;


    public Election() {
        this.ballotOptions = new ArrayList<>();
        this.results = new ArrayList<>();
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


    public void setResults(List<QuestionResult> results) {
        if (results == null) throw new IllegalArgumentException("the list of winners should not be null");
        results.sort((r1, r2) -> r2.getCount().compareTo(r1.getCount()));
        this.results = results;
    }

    public List<QuestionResult> getResults() {
        return results;
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
    public EventType getType() {
        return EventType.ELECTION;
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
