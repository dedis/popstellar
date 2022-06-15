package fe.utils.verification;

import com.intuit.karate.Json;

import static common.JsonKeys.PARAMS;
import static common.utils.JsonUtils.getJSON;
import static fe.utils.verification.VerifierUtils.verifyMessageField;

/**
 * This class contains functions used to test fields specific to Roll-Call
 */
public class RollCallVerification {

  public static boolean rollCallCreateVerification(String message) {
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    return verifyMessageField(paramsFieldJson);
  }
}
