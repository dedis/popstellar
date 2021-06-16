package com.github.dedis.student20_pop.model.network.method.message.data;

import com.google.gson.annotations.SerializedName;

public class QuestionResult {
    @SerializedName(value = "ballot_option")
    private String ballotOption;
    private int count;

    public QuestionResult(String ballotOption, int count) {
        if (ballotOption == null) throw new IllegalArgumentException();
        this.ballotOption = ballotOption;
        this.count = count;
    }

    public String getBallot() {
        return ballotOption;
    }

    public Integer getCount() {
        return count;
    }
}
