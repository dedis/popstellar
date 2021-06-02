package com.github.dedis.student20_pop.model;

import android.util.Log;

import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.model.network.method.message.ElectionQuestion;
import com.github.dedis.student20_pop.model.network.method.message.ElectionVote;
import com.github.dedis.student20_pop.model.network.method.message.QuestionResult;
import com.github.dedis.student20_pop.model.event.EventType;
import com.github.dedis.student20_pop.utility.security.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Election extends Event {

    private String channel;
    private String id;
    private String name;
    private long creation;
    private long start;
    private long end;
    private List<ElectionQuestion> electionQuestions;

    //Map that associates each sender pk to their votes
    private Map<String, List<ElectionVote>> voteMap;

    //Results of an election
    private List<QuestionResult> results;

    public Election() {
        this.results = new ArrayList<>();
        this.electionQuestions = new ArrayList<>();
        this.voteMap = new TreeMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreation() {
        return creation;
    }

    public String getChannel(){ return channel; }

    public List<ElectionQuestion> getElectionQuestions() {
        return electionQuestions;
    }

    public void setCreation(long creation) {
        if (creation < 0) throw new IllegalArgumentException();
        this.creation = creation;
    }

    public void setStart(long start) {
        if (start < 0) throw new IllegalArgumentException();
        this.start = start;
    }

    public void setEnd(long end) {
        if (end < 0) throw new IllegalArgumentException();
        this.end = end;
    }

    public void putSenderVotes(String senderPk, List<ElectionVote> votes) {
        if (senderPk == null) throw new IllegalArgumentException("Sender public key cannot be null.");
        voteMap.put(senderPk, votes);
    }

    public void setChannel(String channel) {this.channel = channel;}

    public void setElectionQuestions(List<ElectionQuestion> electionQuestions) {
        this.electionQuestions = electionQuestions;
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
     * Computes the hash for the registered votes, when terminating an election (ordered by sender pk)
     * @return the hash of all registered votes
     */
    public String computerRegisteredVotes() {
        List<String> listOfVoteIds = new ArrayList<>();
        for (List<ElectionVote> votes: voteMap.values()) {
            for (ElectionVote vote: votes) {
                listOfVoteIds.add(vote.getId());
            }
        }
        System.out.println(listOfVoteIds.toString());
        return Hash.hash(listOfVoteIds.toString());
    }


    public void setResults(List<QuestionResult> results) {
        if (results == null) throw new IllegalArgumentException("the list of winners should not be null");
        results.sort((r1, r2) -> r2.getCount().compareTo(r1.getCount()));
        this.results = results;
    }

    public List<QuestionResult> getResults() {
        return results;
    }

    @Override
    public EventType getType() {
        return EventType.ELECTION;
    }
}
