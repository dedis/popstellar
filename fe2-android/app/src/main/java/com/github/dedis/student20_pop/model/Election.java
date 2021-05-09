package com.github.dedis.student20_pop.model;

import com.github.dedis.student20_pop.model.event.Event;

import java.util.ArrayList;
import java.util.List;

public class Election extends Event {

    private String id;
    private String name;
    private long creation;
    private long start;
    private long end;
    private boolean writeIn;
    private List<String> questions;
    private List<List<String>> ballotsOptions;
    private List<List<Integer>> votes;

    public Election() {
        this.ballotsOptions = new ArrayList<>();
        this.questions = new ArrayList<>();
        votes = new ArrayList<>();
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

    public List<List<Integer>> getVotes() {
        return votes;
    }

    public void setVotes(List<List<Integer>> votes) {
        if(votes == null) throw new IllegalArgumentException("votes cannot be null");
        this.votes = votes;
    }

    public List<List<String>> getBallotsOptions() {
        return ballotsOptions;
    }

    public void setBallotsOptions(List<List<String>> ballotsOptions) {
        if (ballotsOptions == null)
            throw new IllegalArgumentException("ballots options can't be null");
        if (ballotsOptions.isEmpty())
            throw new IllegalArgumentException();
        this.ballotsOptions = ballotsOptions;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        if (questions == null) throw new IllegalArgumentException("question can't be null");
        this.questions = questions;
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
}
