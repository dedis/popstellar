package com.github.dedis.student20_pop.model.network.method.message;

import com.github.dedis.student20_pop.utility.security.Hash;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ElectionVote  {

    private String id; /** Id of the object ElectionVote : Hash(“Vote”||election_id||
     || question_id||(vote_index(es)|write_in))
     **/
    @SerializedName(value = "question")
    private String questionId; // id of the question
    private List<Integer> vote; // list of indexes for the votes
    @SerializedName(value = "write_in")
    private Boolean writeIn; // enables to write in in ballot options

    /**
     * Constructor for a data Question, for the election setup
     * @param questionId the Id of the question
     * @param vote the list of indexes for the ballot options chose by the voter
     * @param writeIn enables write in
     * @param electionId Id of the election
     */
    public ElectionVote(
            String questionId,
            List<Integer> vote,
            Boolean writeIn,
            String electionId) {

        this.questionId = questionId;
        this.writeIn = writeIn;
        this.vote = vote;
        this.id = Hash.hash("Vote", electionId, questionId, vote.toString(), writeIn.toString());
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
        return "ElectionQuestion{"
                + "id='"
                + id
                + '\''
                + ", question ID='"
                + questionId
                + '\''
                + ", votes='"
                + vote
                + '\''
                + ", write in='"
                + writeIn
                + '}';
    }


}

