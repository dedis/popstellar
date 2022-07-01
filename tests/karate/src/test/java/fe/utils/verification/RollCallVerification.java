package fe.utils.verification;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;

import java.security.NoSuchAlgorithmException;

import static common.utils.Constants.*;
import static common.utils.JsonUtils.getJSON;
import static fe.utils.verification.VerificationUtils.getMsgDataJson;

/**
 * This class contains functions used to test fields specific to Roll-Call
 */
public class RollCallVerification {
  private final static Logger logger = new Logger(RollCallVerification.class.getSimpleName());

//  /**
//   * Verifies that the object of the roll call creation message is "roll_call"
//   * @param message the network message
//   * @return true if the object of the roll call creation message is "roll_call"
//   */
// public boolean verifyObject(String message){
//   Json data = getMsgDataJson(message);
//   return ROLL_CALL.equals(data.get(OBJECT));
// }
//
//  /**
//   * Verifies that the action of the roll call creation message is "creation"
//   * @param message the network message
//   * @return true if the action of the roll call creation message is "creation"
//   */
// public boolean verifyCreateAction(String message){
//   Json data = getMsgDataJson(message);
//   return CREATE.equals(data.get(ACTION));
// }

  /**
   * Verifies that the roll call id is computed as expected
   * @param message the message sent over the network
   * @return true if the roll call id field matches expectations
   */
 public boolean verifyRollCallId(String message){
   String laoId = getLaoId(message);
   Json createMessageJson = getMsgDataJson(message);

   return verifyRollCallId(createMessageJson, laoId);
 }

  /**
   * Verifies if the name specified in the network message matches the one provided
   * @param message the message sent over the network
   * @param name the name to be compared to
   * @return true if the name contained in message matches the one in argument
   */
 public boolean verifyRollCallName(String message, String name){
   Json createMessageJson = getMsgDataJson(message);
   logger.info("name in arguement is " + name);
   logger.info("name in json is " +createMessageJson.get(NAME));
   return name.equals(createMessageJson.get(NAME));
 }

  //////////////////////////////////////////////////// Utils ///////////////////////////////////////////////////////////

  private String getLaoId(String message){
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    String channel = paramsFieldJson.get(CHANNEL);

    // The laoId is the channel without leading /root/ and end \ characters
    return channel.replace("/root/", "").replace("\\", "");
  }

  private boolean verifyRollCallId(Json createMessageJson, String laoId){
    String rcId = createMessageJson.get(ID);
    String creation = getStringFromIntegerField(createMessageJson, CREATION);
    String rcName = createMessageJson.get(NAME);

    try {
      return rcId.equals(JsonConverter.hash("R".getBytes(), laoId.getBytes(), creation.getBytes(), rcName.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  /**
   * Because of internal type used by karate, doing casting in 2 steps is required
   */
  private String getStringFromIntegerField(Json json, String key){
    Integer intTemp = json.get(key);
    return String.valueOf(intTemp);
  }
}
