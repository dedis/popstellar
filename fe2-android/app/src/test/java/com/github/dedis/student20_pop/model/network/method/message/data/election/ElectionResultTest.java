package com.github.dedis.student20_pop.model.network.method.message.data.election;

import com.github.dedis.student20_pop.model.network.method.message.ElectionResultQuestion;
import com.github.dedis.student20_pop.model.network.method.message.QuestionResult;
import com.github.dedis.student20_pop.model.network.method.message.data.Action;
import com.github.dedis.student20_pop.model.network.method.message.data.Objects;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ElectionResultTest {
    private List<QuestionResult> results = Arrays.asList(new QuestionResult("Candidate1", 40));
    private List<ElectionResultQuestion> questions = Arrays.asList(new ElectionResultQuestion("question id", results));
    private ElectionResult electionResult = new ElectionResult(questions);

    @Test
    public void questionsCantBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new ElectionResult(null));
    }

    @Test
    public void questionsCantBeEmpty() {
        List<ElectionResultQuestion> emptyList = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> new ElectionResult(emptyList));
    }

    @Test
    public void electionResultGetterReturnsCorrectQuestions() {
        assertThat(electionResult.getElectionQuestionResults(), is(questions));
    }

    @Test
    public void electionResultGetterReturnsCorrectObject() {
        assertThat(electionResult.getObject(), is(Objects.ELECTION.getObject()));
    }

    @Test
    public void electionResultGetterReturnsCorrectAction() {
        assertThat(electionResult.getAction(), is(Action.RESULT.getAction()));
    }
}
