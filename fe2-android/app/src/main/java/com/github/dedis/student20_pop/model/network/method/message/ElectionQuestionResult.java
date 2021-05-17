package com.github.dedis.student20_pop.model.network.method.message;

import java.util.Map;

public class ElectionQuestionResult {

    private String id;
    private Map<String, Integer> results;

    public ElectionQuestionResult(String id, Map<String, Integer> results) {
        if (id == null || results == null || results.isEmpty()) throw new IllegalArgumentException();
        this.id = id;
        this.results = results;
    }

    public String getId() {
        return id;
    }

    public Map<String, Integer> getResults() {
        return results;
    }
}

