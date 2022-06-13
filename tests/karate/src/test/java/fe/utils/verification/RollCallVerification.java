package fe.utils.verification;

import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import common.utils.Base64Utils;

import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static common.JsonKeys.*;
import static common.utils.JsonUtils.getJSON;

public class RollCallVerification {
  private final static Logger logger = new Logger(RollCallVerification.class.getSimpleName());

  public static boolean rollCallCreateVerification(String message) throws NoSuchAlgorithmException {
    logger.info("message is " + message);

    Json json = getJSON(Json.of(message), "params");
    Map map = json.get("message");
    Json msg = Json.of(map);
    boolean msgIdValidity = verifyMsgId(msg);
    String data = msg.get("data");
    logger.info("data " + data);
    logger.info("converter try is " + Base64Utils.convertB64ToString(data));

    return msgIdValidity;
  }

  private static boolean verifyMsgId(Json msgFieldJson) throws NoSuchAlgorithmException {
    String data = msgFieldJson.get(DATA);
    String signature = msgFieldJson.get(SIGNATURE);
    String msgId = msgFieldJson.get(MESSAGE_ID);
    return VerifierUtils.verifyMessageId(data, signature, msgId);
  }
}
