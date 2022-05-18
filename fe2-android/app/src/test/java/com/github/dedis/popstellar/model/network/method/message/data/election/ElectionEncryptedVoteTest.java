package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import com.github.dedis.popstellar.utility.security.Hash;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ElectionEncryptedVoteTest {

    private final String electionId = "my election id";
    private final String questionId = " my question id";
    // we vote for ballot option in position 2, then posiion 1 and 0
    private final List<String> votes =
            new ArrayList<>(
                    Arrays.asList("2", "1", "0"));
    private final String writeIn = "My write in ballot option";
    private final ElectionEncryptedVote electionEncryptedVote1 =
            new ElectionEncryptedVote(electionId, votes, writeIn, false, questionId);
    private final ElectionEncryptedVote electionEncryptedVotes2 =
            new ElectionEncryptedVote(electionId, votes, writeIn, true, questionId);

    @Test
    public void electionVoteWriteInDisabledReturnsCorrectId() {
        // WriteIn enabled so id is Hash('Vote'||election_id||question_id||write_in)
        String expectedId =
                Hash.hash(
                        "Vote", electionId, electionEncryptedVote1.getQuestionId(), electionEncryptedVote1.getVote().toString());
        assertThat(electionEncryptedVote1.getId(), is(expectedId));
    }

    @Test
    public void electionVoteWriteInEnabledReturnsCorrectId() {
        // WriteIn enabled so id is Hash('Vote'||election_id||question_id)
        // Hash code shouldn't change with new protocol specifications
        String expectedId =
                Hash.hash("Vote", electionId, electionEncryptedVotes2.getQuestionId());
        assertThat(electionEncryptedVotes2.getId().equals(expectedId), is(false));
        assertNull(electionEncryptedVotes2.getVote());
    }

    @Test
    public void getIdTest() {
        assertThat(electionEncryptedVote1.getQuestionId(), is(questionId));
    }

    @Test
    public void attributesIsNull() {
        assertNull(electionEncryptedVotes2.getVote());
        assertNotNull(electionEncryptedVote1.getVote());
    }

    @Test
    public void getVoteTest() {
        assertThat(electionEncryptedVote1.getVote(), is(votes));
    }

    @Test
    public void isEqual() {
        assertNotEquals(electionEncryptedVote1, electionEncryptedVotes2);
        assertEquals(electionEncryptedVote1, new ElectionEncryptedVote(questionId, votes, writeIn, false, electionId));
        assertNotEquals(electionEncryptedVote1, new ElectionEncryptedVote("random", votes, writeIn, false, electionId));
        assertNotEquals(
                electionEncryptedVote1,
                new ElectionEncryptedVote(
                        questionId, new ArrayList<>(Arrays.asList("0","1", "2")), writeIn, false, electionId));
        assertNotEquals(electionEncryptedVote1, new ElectionEncryptedVote(electionId, votes, writeIn, false, "random"));

        // here because writeInEnabled is false it will be computed as null making both elections the
        // same even though we don't give them the same constructor
        assertEquals(electionEncryptedVote1, new ElectionEncryptedVote(electionId, votes, "random", false, questionId));

        // here because writeInEnabled is true the list of votes should be computed as null making both
        // election the same
        assertEquals(
                electionEncryptedVotes2,
                new ElectionEncryptedVote(
                        electionId, new ArrayList<>(Arrays.asList("0", "1", "2")), writeIn, true, questionId));
    }


}