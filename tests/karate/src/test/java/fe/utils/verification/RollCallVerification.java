package fe.utils.verification;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;
import common.utils.Base64Utils;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import static common.JsonKeys.*;
import static common.utils.JsonUtils.getJSON;

/** This class contains functions used to test fields specific to Roll-Call */
public class RollCallVerification {
  private static final Logger logger = new Logger(RollCallVerification.class.getSimpleName());

  /**
   * Verifies that the roll call creation message data is valid. In particular that it is a roll
   * call open message and that the roll call id is coherently computed
   *
   * @param message the message sent over the network
   * @return true if the aforementioned fields match expectations
   */
  public static boolean verifyCreate(String message) {
    String laoId = getLaoId(message);
    Json createMessageJson = getMsgDataJson(message);

    boolean objectFieldCorrectness = ROLL_CALL.equals(createMessageJson.get(OBJECT));
    boolean actionFieldCorrectness =
        CREATE.equals(createMessageJson.get(ACTION)); // Check that the action matches
    return actionFieldCorrectness
        && objectFieldCorrectness
        && verifyRollCallId(createMessageJson, laoId);
  }

  /**
   * Verifies that the roll call open message data is valid. In particular that it is a roll call
   * open message and that the roll call update_id is coherently computed
   *
   * @param message the message sent over the network
   * @return true if the aforementioned fields match expectations
   */
  public static boolean verifyOpen(String message, boolean reopen) {
    String laoId = getLaoId(message);
    Json openMessageJson = getMsgDataJson(message);

    boolean objectFieldCorrectness = ROLL_CALL.equals(openMessageJson.get(OBJECT));
    String action = reopen ? REOPEN : OPEN;
    boolean actionFieldCorrectness = action.equals(openMessageJson.get(ACTION));

    return actionFieldCorrectness
        && objectFieldCorrectness
        && verifyUpdateId(openMessageJson, laoId, OPENS, OPENED_AT);
  }

  public static boolean verifyCloses(String message) {
    String laoId = getLaoId(message);
    Json closeMessageJson = getMsgDataJson(message);
    List<String> attendees = closeMessageJson.get(ATTENDEES);
    logger.info("attendees are " + attendees.toString());

    boolean objectFieldCorrectness = ROLL_CALL.equals(closeMessageJson.get(OBJECT));
    boolean actionFieldCorrectness = CLOSE.equals(closeMessageJson.get(ACTION));
    boolean updateIdCorrectness = verifyUpdateId(closeMessageJson, laoId, CLOSES, CLOSED_AT);
    logger.info("object " + objectFieldCorrectness + " action " + actionFieldCorrectness + " update " + updateIdCorrectness);
    return objectFieldCorrectness && actionFieldCorrectness && updateIdCorrectness && attendees.size()==1;
  }

  ////////////////////// Utils //////////////////////

  private static String getDataFromTopLevel(String message) {
    // We break down each level for clarity
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    Json messageFieldJson = getJSON(paramsFieldJson, MESSAGE);
    return messageFieldJson.get(DATA);
  }

  private static Json getMsgDataJson(String message) {
    String b64Data = getDataFromTopLevel(message);
    String data = new String(Base64Utils.convertB64URLToByteArray(b64Data));
    return Json.of(data);
  }

  private static String getLaoId(String message) {
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    String channel = paramsFieldJson.get(CHANNEL);

    // The laoId is the channel without leading /root/ and end \ characters
    return channel.replace("/root/", "").replace("\\", "");
  }

  private static boolean verifyRollCallId(Json createMessageJson, String laoId) {
    String rcId = createMessageJson.get(ID);
    String creation = getStringFromIntegerField(createMessageJson, CREATION);
    String rcName = createMessageJson.get(NAME);

    try {
      return rcId.equals(
          JsonConverter.hash(
              "R".getBytes(), laoId.getBytes(), creation.getBytes(), rcName.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  /**
   * Verifies the update_id of a roll-call message
   *
   * @param rollCallMessageJson the message_data of the roll-call message
   * @param laoId the laoId of the LAO in which the roll-call is taking place
   * @param referenceKey is either closes or opens depending on the action
   * @param timeKey is either opened_at or closed_at depending on the action
   * @return true if the computed id matches the one provided in the message_data
   */
  private static boolean verifyUpdateId(
      Json rollCallMessageJson, String laoId, String referenceKey, String timeKey) {

    String updateId = rollCallMessageJson.get(UPDATE_ID);
    String reference = rollCallMessageJson.get(referenceKey);
    String time = getStringFromIntegerField(rollCallMessageJson, timeKey);

    try {
      return updateId.equals(
          JsonConverter.hash(
              "R".getBytes(), laoId.getBytes(), reference.getBytes(), time.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  /** Because of internal type used by karate, doing casting in 2 steps is required */
  private static String getStringFromIntegerField(Json json, String key) {
    Integer intTemp = json.get(key);
    return String.valueOf(intTemp);
  }
}
