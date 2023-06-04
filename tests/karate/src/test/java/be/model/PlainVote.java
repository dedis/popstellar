package be.model;

/**
 * Represents a non-encrypted vote for one question.
 */
public class PlainVote {

  // ID of the vote
  // Hash('Vote'||election_id||question_id||(vote_index(es)|write_in))
  public String id;

  // ID of the object ElectionVote
  public String questionId;

  // index of the chosen vote
  public Integer index;

  public PlainVote(String questionId, Integer index, boolean writeInEnabled, String writeIn, String electionId){
    this.questionId = questionId;
    this.index = writeInEnabled ? null : index;
    this.id = Election.generateElectionVoteId(electionId, questionId, index, writeIn, writeInEnabled);
  }

  public String getId(){
    return id;
  }
}
