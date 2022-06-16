package fe.utils.verification;

import com.intuit.karate.Json;

import static common.JsonKeys.PARAMS;
import static common.utils.JsonUtils.getJSON;
import static fe.utils.verification.VerifierUtils.verifyMessageField;

public class PublishMessageVerification {

  public static boolean verifyPublishMessage(String message) {
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    return verifyMessageField(paramsFieldJson);
  }
}
