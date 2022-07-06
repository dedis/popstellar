package fe.utils.verification;

import be.utils.JsonConverter;
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
   * Verfies that the election id is coherently computed
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
      JsonConverter jsonConverter = new JsonConverter();
      return electionId.equals(
          jsonConverter.hash(
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
   * Verfies that the question id is coherently computed
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
      JsonConverter jsonConverter = new JsonConverter();
      return questionId.equals(
          jsonConverter.hash(
              "Question".getBytes(),
              electionId.getBytes(),
              question.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  public boolean verifyVoteId(String message, int index){
    Json voteMessageJson = getMsgDataJson(message);
    Json voteJson = getVotes(message);

    String electionId = voteMessageJson.get(constants.ELECTION);
    String questionId = voteJson.get(QUESTION);
    String voteId = voteJson.get(ID);
    String vote = getStringFromIntegerField(voteJson, VOTE);

    try {
      JsonConverter jsonConverter = new JsonConverter();
      return voteId.equals(
          jsonConverter.hash(
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

  public String getVote(String message){
    Json votes = getVotes(message);
    return getStringFromIntegerField(votes, VOTE);
  }

  private Json getElectionQuestion(String message){
    Json setupMessageJson = getMsgDataJson(message);
    List<String> questionArray = setupMessageJson.get(QUESTIONS);
    return Json.of(questionArray.get(0));
  }

  private Json getVotes(String message){
    Json msgJson = getMsgDataJson(message);
    List<String> questionArray = msgJson.get(VOTES);
    logger.info("question array is " + questionArray);
    return Json.of(questionArray.get(0));
  }
}
