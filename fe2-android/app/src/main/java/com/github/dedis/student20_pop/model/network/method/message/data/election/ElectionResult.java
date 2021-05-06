package com.github.dedis.student20_pop.model.network.method.message.data.election;

import com.github.dedis.student20_pop.model.network.method.message.ElectionQuestionResult;
import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Data;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;

import java.util.List;

public class ElectionResult extends Data {

    private List<ElectionQuestionResult> questions;

    @Override
    public String getObject() {
        return Objects.ELECTION.getObject();
    }

    @Override
    public String getAction() {
        return Action.RESULT.getAction();
    }

    public List<ElectionQuestionResult> getElectionQuestionResults() {
        return questions;
    }
}
