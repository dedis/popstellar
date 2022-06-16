package fe.utils.verification;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import common.utils.Base64Utils;

import java.security.NoSuchAlgorithmException;

import static common.JsonKeys.*;
import static common.utils.JsonUtils.getJSON;

/**
 * This class contains functions used to test fields specific to Roll-Call
 */
public class RollCallVerification {
  private final static Logger logger = new Logger(RollCallVerification.class.getSimpleName());

  public static boolean verifyCreate(String message) {
    String b64Data = getDataFromTopLevel(message);
    String data = new String(Base64Utils.convertB64URLToByteArray(b64Data));
    String laoId = getLaoId(message);
    logger.info("Decoded : " + data);
    Json createMessageJson = Json.of(data);

    boolean objectFieldCorrectness = ROLL_CALL.equals(createMessageJson.get(OBJECT));
    boolean actionFieldCorrectness = CREATE.equals(createMessageJson.get(ACTION)); //Check that the action matches
    return actionFieldCorrectness && objectFieldCorrectness && verifyRollCallId(createMessageJson, laoId);
  }

  public static boolean verifyOpen(String message){
    return false;
  }

  private static String getDataFromTopLevel(String message){
    // We break down each level for clarity
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    Json messageFieldJson = getJSON(paramsFieldJson, MESSAGE);
    return messageFieldJson.get(DATA);
  }

  private static String getLaoId(String message){
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    String channel = paramsFieldJson.get(CHANNEL);

    // The laoId is the channel without leading /root/ and end \ characters
    return channel.replace("/root/", "").replace("\\", "");
  }

  private static boolean verifyRollCallId(Json createMessageJson, String laoId){
    String rcId = createMessageJson.get(ID);

    // Because of internal type use by karate, doing casting in 2 steps is required
    Integer creationInt = (createMessageJson.get(CREATION));
    String creation = String.valueOf(creationInt);

    String rcName = createMessageJson.get(NAME);
    try {
      return rcId.equals(JsonConverter.hash("R".getBytes(), laoId.getBytes(), creation.getBytes(), rcName.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }
}
