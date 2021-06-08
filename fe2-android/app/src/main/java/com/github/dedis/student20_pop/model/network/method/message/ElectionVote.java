package com.github.dedis.student20_pop.model.network.method.message;

import com.github.dedis.student20_pop.utility.security.Hash;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ElectionVote {

    private String id;
    /**
     * Id of the object ElectionVote : Hash(“Vote”||election_id||
     * || question_id||(vote_index(es)|write_in))
     **/
    @SerializedName(value = "question")
    private String questionId; // id of the question
    private List<Integer> vote; // list of indexes for the votes
    private boolean writeInEnabled; // represents a boolean to know whether write_in is allowed or not
    @SerializedName(value = "write_in")
    private String writeIn; // If write in is enabled this represents the writeIn string

    /**
     * Constructor for a data Vote, for cast vote . It represents a Vote for one Question.
     *
     * @param questionId     the Id of the question
     * @param vote           the list of indexes for the ballot options chose by the voter
     * @param writeInEnabled parameter to know if write is enabled or not
     * @param writeIn        string corresponding to the write_in
     * @param electionId     Id of the election
     */
    public ElectionVote(
            String questionId,
            List<Integer> vote,
            boolean writeInEnabled,
            String writeIn,
            String electionId) {

        this.questionId = questionId;
        this.writeInEnabled = writeInEnabled;
        if (writeInEnabled) { // If write in is enabled the Id is formed with the write_in string
            this.writeIn = writeIn;
            this.vote = null;
            this.id = Hash.hash("Vote", electionId, questionId, writeIn);
        } else { // If write in is not enabled the Id is formed with the vote indexes
            this.writeIn = null;
            this.vote = vote;
            this.id = Hash.hash("Vote", electionId, questionId, vote.toString());
        }
    }


    public String getId() {
        return id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getWriteIn() {
        return writeIn;
    }

    public List<Integer> getVotes() {
        return vote;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ElectionVote that = (ElectionVote) o;
        return getQuestionId() == that.getQuestionId()
                && getWriteIn() == that.getWriteIn()
                && java.util.Objects.equals(getId(), that.getId())
                && java.util.Objects.equals(getVotes(), that.getVotes());

    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                getId(),
                getVotes(),
                getWriteIn(),
                getQuestionId());
    }

    @Override
    public String toString() {
        if (writeInEnabled) {
            return "ElectionQuestion{"
                    + "id='"
                    + id
                    + '\''
                    + ", question ID='"
                    + questionId
                    + '\''
                    + ", write in='"
                    + writeIn
                    + '}';
        } else {
            return "ElectionQuestion{"
                    + "id='"
                    + id
                    + '\''
                    + ", question ID='"
                    + questionId
                    + '\''
                    + ", votes='"
                    + vote.toString()
                    + '}';

        }

    }


}

