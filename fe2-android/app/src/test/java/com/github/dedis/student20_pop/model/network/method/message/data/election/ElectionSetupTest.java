package com.github.dedis.student20_pop.model.network.method.message.data.election;

import android.util.Base64;

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
public class ElectionSetupTest {

    private String electionSetupName = "new election setup";
    private long start = 0;
    private long end = 1;
    private String votingMethod = "Plurality";
    private boolean writeIn = false;
    private List<String> ballotOptions = Arrays.asList("candidate1", "candidate2");
    private String question = "which is the best ?";
    private String laoId = "my lao id";
    private ElectionSetup electionSetup;

    @Before
    public void setup() {
        PowerMockito.mockStatic(Base64.class);
        when(Base64.encodeToString(any(), anyInt())).thenAnswer(invocation -> java.util.Base64.getUrlEncoder().encodeToString((byte[]) invocation.getArguments()[0]));
        electionSetup = new ElectionSetup(electionSetupName, start, end, votingMethod, writeIn, ballotOptions, question, laoId);
    }

    @Test
    public void electionSetupGetterReturnsCorrectName() {
        assertThat(electionSetup.getName(), is(electionSetupName));
    }

    @Test
    public void electionSetupGetterReturnsCorrectStartTime() {
        assertThat(electionSetup.getStartTime(), is(start));
    }

    @Test
    public void electionSetupGetterReturnsCorrectEndTime() {
        assertThat(electionSetup.getEndTime(), is(end));
    }

    @Test
    public void electionSetupGetterReturnsCorrectLaoId() {
        assertThat(electionSetup.getLao(), is(laoId));
    }

    @Test
    public void electionSetupOnlyOneQuestion() {
        assertThat(electionSetup.getQuestions().size(), is(1));
    }

}
