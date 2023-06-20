package be.model;

import java.util.List;

/** Simplified version of an election question, used to generate valid election question data. */
public class ElectionQuestion {

  public String id;
  public String question;
  public String votingMethod;
  public List<String> ballotOptions;
  public boolean writeIn;
  public String electionId;

  public ElectionQuestion(String electionId, String question, String votingMethod, List<String> ballotOptions, boolean writeIn) {
    this.question = question;
    this.votingMethod = votingMethod;
    this.ballotOptions = List.copyOf(ballotOptions);
    this.writeIn = writeIn;
    this.id = Election.generateElectionQuestionId(electionId, this.question);
    this.electionId = electionId;
  }

  /**
   * Create a vote for the ballot option at index voteIndex for this question.
   * Does not check that voteIndex is within bounds to allow for testing this case.
   *
   * @param voteIndex the index of the ballot option to vote for
   * @return a valid vote for this ballot option
   */
  public PlainVote createVote(int voteIndex){
    return new PlainVote(id, voteIndex, false, null, electionId);
  }
}
