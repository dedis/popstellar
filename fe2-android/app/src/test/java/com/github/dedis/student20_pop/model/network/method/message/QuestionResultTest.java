package com.github.dedis.student20_pop.model.network.method.message;

import com.github.dedis.student20_pop.model.network.method.message.data.QuestionResult;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;

public class QuestionResultTest {

    private Integer count = 30;
    private String name = "Candidate1";
    private QuestionResult questionResult = new QuestionResult(name, count);

    @Test
    public void fieldsCantBeNull() {
        assertThrows(IllegalArgumentException.class, () -> new QuestionResult(null, 30));
        assertThrows(IllegalArgumentException.class, () -> new QuestionResult("Candidate1", null));
    }

    @Test
    public void questionResultGetterReturnsCorrectName() {
        assertThat(questionResult.getName(), is(name));
    }

    @Test
    public void questionResultGetterReturnsCorrectCount() {
        assertThat(questionResult.getCount(), is(count));
    }
}
