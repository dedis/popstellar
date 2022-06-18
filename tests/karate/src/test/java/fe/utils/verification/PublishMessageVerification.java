package fe.utils.verification;

import com.intuit.karate.Json;

import static common.JsonKeys.PARAMS;
import static common.utils.JsonUtils.getJSON;
import static fe.utils.verification.VerifierUtils.verifyMessageField;

/**
 *  This class contains a function used to checking fields of a publish message, regardless
 *  of the message data it contains
 */
public class PublishMessageVerification {

  /**
   * Checks that the message_id and signature match the sender and the data
   * @param message The string representation of a publish message json
   * @return true if the aforementioned fields match expectations
   */
  public static boolean verifyPublishMessage(String message) {
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    return verifyMessageField(paramsFieldJson);
  }
}
