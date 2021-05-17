package com.github.dedis.student20_pop.model.network.method.message;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class ElectionQuestionResultTest {
    private String questionId = "questionId";
    private Map<String, Integer> results =  new HashMap<String, Integer>() {{put("Candidate1", 30);}};
    private ElectionQuestionResult electionQuestionResult = new ElectionQuestionResult(questionId, results);

    @Test
    public void electionQuestionGetterReturnsCorrectQuestionId() {
        assertThat(electionQuestionResult.getId(), is(questionId));
    }

    @Test
    public void electionQuestionGetterReturnsCorrectResults() {
        assertThat(electionQuestionResult.getResults(), is(results));
    }

    @Test
    public void fieldsCantBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new ElectionQuestionResult(null, results));
        assertThrows(IllegalArgumentException.class, () -> new ElectionQuestionResult(questionId, null));
    }

    @Test
    public void resultsCantBeEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new ElectionQuestionResult(questionId, new HashMap<>()));
    }
}
