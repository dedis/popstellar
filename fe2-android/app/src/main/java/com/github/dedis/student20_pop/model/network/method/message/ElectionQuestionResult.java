package com.github.dedis.student20_pop.model.network.method.message;

import java.util.List;

public class ElectionQuestionResult {

    private String id;
    private List<String> results;

    public ElectionQuestionResult(String id, List<String> results) {
        this.id = id;
        this.results = results;
    }

    public String getId() {
        return id;
    }

    public List<String> getResults() {
        return results;
    }

}
