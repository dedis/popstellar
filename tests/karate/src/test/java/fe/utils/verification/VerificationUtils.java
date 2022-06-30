package fe.utils.verification;

import com.intuit.karate.Json;
import common.utils.Base64Utils;
import common.utils.JsonUtils;

import static common.JsonKeys.*;

/**
 * This class contains useful utils functions for the verification process
 */
public class VerificationUtils {

  /**
   * Returns the Json object containing the "message" field of the provided network message
   * @param message the network message
   * @return the Json object containing the "message" field
   */
  public static Json getMessageFieldFromMessage(String message){
    Json jsonMessage = Json.of(message);
    Json paramFieldJson = JsonUtils.getJSON(jsonMessage, PARAMS);
    return Json.of(paramFieldJson.get(MESSAGE));
  }

  /**
   * Returns the "data" field of the provided network message
   * @param message the network message
   * @return the "data" field in base64 format
   */
  public static String getDataFieldFromMessage(String message){
    Json messageField = getMessageFieldFromMessage(message);
    return messageField.get(DATA);
  }

  /**
   * Returns the Json object containing the decoded "data" field of the provided network message
   * @param message the network message
   * @return the Json object containing the decoded "data" field
   */
  public static Json getMsgDataJson(String message){
    String b64Data = getDataFieldFromMessage(message);
    String data = new String(Base64Utils.convertB64URLToByteArray(b64Data));
    return Json.of(data);
  }
}
