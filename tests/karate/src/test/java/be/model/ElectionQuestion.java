package be.model;

import java.util.List;

public class ElectionQuestion {

  public String id;
  public String question;
  public String votingMethod;
  public List<String> ballotOptions;
  public boolean writeIn;

  public ElectionQuestion(String electionId, String question, String votingMethod, List<String> ballotOptions, boolean writeIn) {
    this.question = question;
    this.votingMethod = votingMethod;
    this.ballotOptions = List.copyOf(ballotOptions);
    this.writeIn = writeIn;
    this.id = Election.generateElectionQuestionId(electionId, this.question);
  }
}
