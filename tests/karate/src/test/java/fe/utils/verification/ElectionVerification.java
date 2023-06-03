package fe.utils.verification;

import be.utils.Hash;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import common.utils.Constants;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import static common.utils.Constants.*;
import static fe.utils.verification.VerificationUtils.getMsgDataJson;
import static fe.utils.verification.VerificationUtils.getStringFromIntegerField;

/** This class contains functions used to test fields specific to Roll-Call */
public class ElectionVerification {
  private static final Logger logger = new Logger(ElectionVerification.class.getSimpleName());
  private Constants constants = new Constants();

  /**
   * Verifies that the election id is coherently computed
   * @param message the network message
   * @return true if the computed election id matches what is expected
   */
  public boolean verifyElectionId(String message) {
    Json setupMessageJson = getMsgDataJson(message);

    String electionId = setupMessageJson.get(ID);
    String createdAt = getStringFromIntegerField(setupMessageJson, CREATED_AT);
    String laoId = setupMessageJson.get(LAO);
    String electionName = setupMessageJson.get(NAME);

    try {
      return electionId.equals(
          Hash.hash(
              "Election".getBytes(),
              laoId.getBytes(),
              createdAt.getBytes(),
              electionName.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  /**
   * Verifies that the question id is coherently computed
   * @param message the network message
   * @return true if the computed question id matches what is expected
   */
  public boolean verifyQuestionId(String message){
    Json setupMessageJson = getMsgDataJson(message);
    Json questionJson = getElectionQuestion(message);

    String electionId = setupMessageJson.get(ID);
    String questionId = questionJson.get(ID);
    String question = questionJson.get(QUESTION);

    try {
      return questionId.equals(
          Hash.hash(
              "Question".getBytes(),
              electionId.getBytes(),
              question.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  /**
   * Verifies that the vote id is coherently computed
   * @param message the network message
   * @param index the index of the ballot that was selected
   * @return true if the computed question id matches what is expected
   */
  public boolean verifyVoteId(String message, int index){
    Json voteMessageJson = getMsgDataJson(message);
    Json voteJson = getVotes(message);

    String electionId = voteMessageJson.get(constants.ELECTION);
    String questionId = voteJson.get(QUESTION);
    String voteId = voteJson.get(ID);
    String vote = getStringFromIntegerField(voteJson, VOTE);

    try {
      return voteId.equals(
          Hash.hash(
              "Vote".getBytes(),
              electionId.getBytes(),
              questionId.getBytes(),
              vote.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  public String getQuestionContent(String message){
    Json questionJson = getElectionQuestion(message);
    return questionJson.get(QUESTION);
  }

  public String getVotingMethod(String message){
    Json questionJson = getElectionQuestion(message);
    return questionJson.get(VOTING_METHOD);
  }

  public String getBallotOption(String message, int index){
    Json questionJson = getElectionQuestion(message);
    List<String> ballots = questionJson.get(BALLOT_OPTIONS);
    return ballots.get(index);
  }

  /**
   * Gets the vote field
   * @param message an element of the "votes" field array of a cast vote message
   * @return the "vote" field of the message in argument
   */
  public String getVote(String message){
    Json votes = getVotes(message);
    return getStringFromIntegerField(votes, VOTE);
  }

  private Json getElectionQuestion(String message){
    Json setupMessageJson = getMsgDataJson(message);
    List<String> questionArray = setupMessageJson.get(QUESTIONS);
    return Json.of(questionArray.get(0));
  }

  /**
   * gets the first element of the "votes" field of a cast vote network message
   * @param message the network message
   * @return the first element of the "votes" field of a cast vote network message
   */
  private Json getVotes(String message){
    Json msgJson = getMsgDataJson(message);
    List<String> questionArray = msgJson.get(VOTES);
    return Json.of(questionArray.get(0));
  }
}
