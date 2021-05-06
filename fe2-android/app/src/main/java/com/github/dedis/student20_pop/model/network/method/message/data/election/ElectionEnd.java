package com.github.dedis.student20_pop.model.network.method.message.data.election;

import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.google.gson.annotations.SerializedName;

public class ElectionEnd extends Data {

    @SerializedName(value = "election")
    private String electionId;
    @SerializedName(value = "created_at")
    private long createdAt;
    @SerializedName(value = "lao")
    private String laoId;


    @Override
    public String getObject() {
        return null;
    }

    @Override
    public String getAction() {
        return null;
    }
}
