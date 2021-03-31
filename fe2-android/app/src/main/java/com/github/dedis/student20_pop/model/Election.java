package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.model.event.Event;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Election extends Event {

    private String id;
    private String name;
    private long creation;
    private long start;
    private long end;
    private boolean writeIn;
    private String question;
    private List<String> ballotOptions;

    //votes as attribute ?


    public Election() { this.ballotOptions = new ArrayList<>(); }

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

    public void setCreation(long creation) {
        this.creation = creation;
    }

    public long getStart() { return start; }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {return end;}

    public void setEnd(long end) {
        this.end = end;
    }

    public List<String> getBallotOptions() {
        return ballotOptions;
    }

    public void setBallotOptions(List<String> ballotOptions) {
        this.ballotOptions = ballotOptions;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public boolean getWriteIn() { return writeIn; }

    public void setWriteIn(boolean writeIn) {this.writeIn = writeIn; }

    @Override
    public long getStartTimestamp() {
        return start;
    }

    @Override
    public long getEndTimestamp() {
        return end;
    }
}
