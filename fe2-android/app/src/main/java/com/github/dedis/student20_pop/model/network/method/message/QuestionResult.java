package com.github.dedis.student20_pop.model.network.method.message;

public class QuestionResult {
    private String name;
    private Integer count;

    public QuestionResult(String name, Integer count) {
        if (name == null || count == null) throw new IllegalArgumentException();
        this.name = name;
        this.count = count;
    }

    public String getName() {
        return name;
    }

    public Integer getCount() {
        return count;
    }
}
