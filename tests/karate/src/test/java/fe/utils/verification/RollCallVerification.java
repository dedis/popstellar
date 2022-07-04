package fe.utils.verification;

import be.utils.JsonConverter;
import com.intuit.karate.Json;
import com.intuit.karate.Logger;

import java.security.NoSuchAlgorithmException;
import java.util.List;

import static common.utils.Constants.*;
import static common.utils.JsonUtils.getJSON;
import static fe.utils.verification.VerificationUtils.getMsgDataJson;

/** This class contains functions used to test fields specific to Roll-Call */
public class RollCallVerification {
  private static final Logger logger = new Logger(RollCallVerification.class.getSimpleName());

  /**
   * Verifies that the roll call id is computed as expected
   *
   * @param message the message sent over the network
   * @return true if the roll call id field matches expectations
   */
  public boolean verifyRollCallId(String message) {
    String laoId = getLaoId(message);
    Json createMessageJson = getMsgDataJson(message);

    return verifyRollCallId(createMessageJson, laoId);
  }

  /**
   * Verifies if the name specified in the network message matches the one provided
   *
   * @param message the message sent over the network
   * @param name the name to be compared to
   * @return true if the name contained in message matches the one in argument
   */
  public boolean verifyRollCallName(String message, String name) {
    Json createMessageJson = getMsgDataJson(message);
    logger.debug("Verifying roll-call id :")
    logger.debug("name in arguement is " + name);
    logger.debug("name in json is " + createMessageJson.get(NAME));
    return name.equals(createMessageJson.get(NAME));
  }

  /**
   * Verifies that the roll call open update_id field is valid
   *
   * @param message the message sent over the network
   * @return true if the update_id field match expectations
   */
  public boolean verifyRollCallUpdateId(String message, String action) {
    String laoId = getLaoId(message);
    Json msgDataJson = getMsgDataJson(message);
    return verifyUpdateId(msgDataJson, laoId, action);
  }

  /**
   * Verifies the presence of the attendees in the network message and that the number of attendees
   * implies the presence of the organizer
   *
   * @param message the network message
   * @param attendees the attendees added
   * @return true if every specified attendees is in the message and if the number of attendees in
   *     the message = number of specified attendees + 1 (for the organizer)
   */
  public boolean verifyAttendeesPresence(String message, String... attendees) {
    Json msgDataJson = getMsgDataJson(message);
    List<String> msgAttendees = msgDataJson.get(ATTENDEES);
    logger.info("Nbr attendees " + attendees.length + " message " + msgAttendees.toString());
    for (String attendee : attendees) {
      if (!msgAttendees.contains(attendee)) {
        return false;
      }
    }
    // The attendee list should be the organizer and all added attendees
    return attendees.length + 1 == msgAttendees.size();
  }

  ////////////////////// Utils //////////////////////

  private boolean verifyRollCallId(Json createMessageJson, String laoId) {
    String rcId = createMessageJson.get(ID);
    String creation = getStringFromIntegerField(createMessageJson, CREATION);
    String rcName = createMessageJson.get(NAME);

    try {
      JsonConverter jsonConverter = new JsonConverter();
      return rcId.equals(
          jsonConverter.hash(
              "R".getBytes(), laoId.getBytes(), creation.getBytes(), rcName.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }

  /** Because of internal type used by karate, doing casting in 2 steps is required */
  private String getStringFromIntegerField(Json json, String key) {
    Integer intTemp = json.get(key);
    return String.valueOf(intTemp);
  }

  private static String getLaoId(String message) {
    Json paramsFieldJson = getJSON(Json.of(message), PARAMS);
    String channel = paramsFieldJson.get(CHANNEL);

    // The laoId is the channel without leading /root/ and end \ characters
    return channel.replace("/root/", "").replace("\\", "");
  }

  /**
   * Verifies the update_id of a roll-call message
   *
   * @param rollCallMessageJson the message_data of the roll-call message
   * @param laoId the laoId of the LAO in which the roll-call is taking place
   * @param action the roll call action (e.g. open)
   * @return true if the computed id matches the one provided in the message_data
   */
  private boolean verifyUpdateId(Json rollCallMessageJson, String laoId, String action) {
    String referenceKey = action.equals(CLOSE_STATIC) ? CLOSES : OPENS;
    String timeKey = action.equals(CLOSE_STATIC) ? CLOSED_AT : OPENED_AT;
    String updateId = rollCallMessageJson.get(UPDATE_ID);
    String reference = rollCallMessageJson.get(referenceKey);
    String time = getStringFromIntegerField(rollCallMessageJson, timeKey);

    try {
      JsonConverter jsonConverter = new JsonConverter();
      return updateId.equals(
          jsonConverter.hash(
              "R".getBytes(), laoId.getBytes(), reference.getBytes(), time.getBytes()));
    } catch (NoSuchAlgorithmException e) {
      logger.info("verification failed with error: " + e);
      return false;
    }
  }
}
