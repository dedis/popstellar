package com.github.dedis.popstellar.model.network.method.message.data.election;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
            new ElectionEncryptedVote(questionId, votes, false, writeIn, electionId);
    private final ElectionEncryptedVote electionEncryptedVotes2 =
            new ElectionEncryptedVote(questionId, votes, true, writeIn, electionId);

    //Hash values util for testing
    String expectedIdNoWriteIn =
            Hash.hash(
                    "Vote", electionId, electionEncryptedVote1.getQuestionId(), electionEncryptedVote1.getVote().toString());
    String expectedIdWithoutWriteIn = Hash.hash("Vote", electionId, electionEncryptedVotes2.getQuestionId());


    @Test
    public void electionVoteWriteInDisabledReturnsCorrectId() {
        // WriteIn enabled so id is Hash('Vote'||election_id||question_id||write_in)
        assertThat(electionEncryptedVote1.getId(), is(expectedIdNoWriteIn));
    }

    @Test
    public void electionVoteWriteInEnabledReturnsCorrectIdTest() {
        // hash = Hash('Vote'||election_id||question_id||encryptedWriteIn)

        assertThat(electionEncryptedVotes2.getId().equals(expectedIdWithoutWriteIn), is(false));
        assertNull(electionEncryptedVotes2.getVote());
    }

    @Test
    public void getIdTest() {
        assertThat(electionEncryptedVote1.getQuestionId(), is(questionId));
    }

    @Test
    public void attributesIsNullTest() {
        assertNull(electionEncryptedVotes2.getVote());
        assertNotNull(electionEncryptedVote1.getVote());
    }

    @Test
    public void getVoteTest() {
        assertThat(electionEncryptedVote1.getVote(), is(votes));
    }

    @Test
    public void isEqualTest() {
        assertNotEquals(electionEncryptedVote1, electionEncryptedVotes2);
        assertEquals(electionEncryptedVote1, new ElectionEncryptedVote(questionId, votes, false, writeIn, electionId));
        assertNotEquals(electionEncryptedVote1, new ElectionEncryptedVote(questionId, votes, false, writeIn, "random"));
        assertNotEquals(
                electionEncryptedVote1,
                new ElectionEncryptedVote(
                        questionId, new ArrayList<>(Arrays.asList("0", "1", "2")), false, writeIn, electionId));
        assertNotEquals(electionEncryptedVote1, new ElectionEncryptedVote("random", votes, false, writeIn, electionId));

        // Same equals, no write_in
        assertEquals(electionEncryptedVote1, new ElectionEncryptedVote(questionId, votes, false, "random", electionId));

        // Same elections, write_in is the same
        assertEquals(
                electionEncryptedVotes2,
                new ElectionEncryptedVote(
                        questionId, new ArrayList<>(Arrays.asList("0", "1", "2")), true, writeIn, electionId));
    }

    @Test
    public void toStringTest() {
        String format =
                String.format("ElectionEncryptedVote{"
                                + "id='%s', "
                                + "questionId='%s', "
                                + "vote=%s}",
                        expectedIdNoWriteIn,
                        questionId,
                        Arrays.toString(votes.toArray()));
        assertEquals(format, electionEncryptedVote1.toString());
    }

}