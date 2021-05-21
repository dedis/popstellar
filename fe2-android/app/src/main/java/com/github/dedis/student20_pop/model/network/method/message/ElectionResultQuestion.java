package com.github.dedis.student20_pop.model.network.method.message;

import java.util.List;
import java.util.Map;

public class ElectionResultQuestion {

    private String id;
    private List<QuestionResult> results;

    public ElectionResultQuestion(String id, List<QuestionResult> results) {
        if (id == null || results == null || results.isEmpty()) throw new IllegalArgumentException();
        this.id = id;
        this.results = results;
    }

    public String getId() {
        return id;
    }

    public List<QuestionResult> getResults() {
        return results;
    }
}

