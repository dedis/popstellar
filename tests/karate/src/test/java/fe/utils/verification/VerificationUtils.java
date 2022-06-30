package fe.utils.verification;

import com.intuit.karate.Json;
import common.utils.Base64Utils;
import common.utils.JsonUtils;

import static common.JsonKeys.*;

public class VerificationUtils {

  public static Json getMessageFieldFromMessage(String message){
    Json jsonMessage = Json.of(message);
    Json paramFieldJson = JsonUtils.getJSON(jsonMessage, PARAMS);
    return Json.of(paramFieldJson.get(MESSAGE));
  }

  public static String getDataFieldFromMessage(String message){
    Json messageField = getMessageFieldFromMessage(message);
    return messageField.get(DATA);
  }

  public static Json getMsgDataJson(String message){
    String b64Data = getDataFieldFromMessage(message);
    String data = new String(Base64Utils.convertB64URLToByteArray(b64Data));
    return Json.of(data);
  }
}
