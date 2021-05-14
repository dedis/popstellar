package com.github.dedis.student20_pop.model.network.method.message;

import android.util.Base64;

import com.github.dedis.student20_pop.model.network.method.message.data.election.ElectionSetup;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {Base64.class})
public class ElectionQuestionTest {

    private String electionId = "my election id";
    private String votingMethod = "Plurality";
    private boolean writeIn = false;
    private List<String> ballotOptions = Arrays.asList("candidate1", "candidate2");
    private String question = "which is the best ?";

    ElectionQuestion electionQuestion;

    @Before
    public void setup() {
        PowerMockito.mockStatic(Base64.class);
        when(Base64.encodeToString(any(), anyInt())).thenAnswer(invocation -> java.util.Base64.getUrlEncoder().encodeToString((byte[]) invocation.getArguments()[0]));
        electionQuestion = new ElectionQuestion(question, votingMethod, writeIn, ballotOptions, electionId);
    }

    @Test
    public void electionQuestionGetterReturnsCorrectQuestion() {
        assertThat(electionQuestion.getQuestion(), is(question));
    }

    @Test
    public void electionQuestionGetterReturnsCorrectVotingMethod() {
        assertThat(electionQuestion.getVotingMethod(), is(votingMethod));
    }

    @Test
    public void electionQuestionGetterReturnsCorrectWriteIn() {
        assertThat(electionQuestion.getWriteIn(), is(writeIn));
    }

    @Test
    public void electionQuestionGetterReturnsCorrectBallotOptions() {
        assertThat(electionQuestion.getBallotOptions(), is(ballotOptions));
    }

}
