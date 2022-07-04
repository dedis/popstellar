package fe.utils.verification;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;

import java.security.NoSuchAlgorithmException;

import static common.utils.Constants.*;
import static fe.utils.verification.VerificationUtils.getMsgDataJson;
import static fe.utils.verification.VerificationUtils.getStringFromIntegerField;

/** This class contains functions used to test fields specific to Roll-Call */
public class ElectionVerification {
  private static final Logger logger = new Logger(ElectionVerification.class.getSimpleName());

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

}
