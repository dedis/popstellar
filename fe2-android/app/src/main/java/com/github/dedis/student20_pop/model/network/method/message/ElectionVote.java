package com.github.dedis.student20_pop.model.network.method.message;

import com.github.dedis.student20_pop.utility.security.Hash;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ElectionVote  {

    private String id;
    @SerializedName(value = "question")
    private String questionId; // id of the question
    private List<Integer> votes;
    private Boolean writeIn;

    /**
     * Constructor for a data Question, for the election setup
     */
    public ElectionVote(
            String questionId,
            List<Integer> votes,
            Boolean writeIn,
            String electionId) {

        this.questionId = questionId;
        this.writeIn = writeIn;
        this.votes = votes;
        this.id = Hash.hash("Vote", electionId, questionId, votes.toString(), writeIn.toString());
    }


    public String getId() {
        return id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public Boolean getWriteIn() {
        return writeIn;
    }

    public List<Integer> getVotes() {
        return votes;
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
        return "ElectionQuestion{"
                + "id='"
                + id
                + '\''
                + ", question ID='"
                + questionId
                + '\''
                + ", votes='"
                + votes
                + '\''
                + ", write in='"
                + writeIn
                + '}';
    }


}

