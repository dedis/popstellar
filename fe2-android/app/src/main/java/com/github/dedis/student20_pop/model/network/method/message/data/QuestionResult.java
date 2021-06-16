package com.github.dedis.student20_pop.model.network.method.message.data;

public class QuestionResult {
    private String name;
    private int count;

    public QuestionResult(String name, int count) {
        if (name == null) throw new IllegalArgumentException();
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
