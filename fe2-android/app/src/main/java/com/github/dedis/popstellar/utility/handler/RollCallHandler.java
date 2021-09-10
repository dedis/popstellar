package com.github.dedis.popstellar.utility.handler;

import android.util.Log;
import com.github.dedis.popstellar.model.Lao;
import com.github.dedis.popstellar.model.RollCall;
import com.github.dedis.popstellar.model.WitnessMessage;
import com.github.dedis.popstellar.model.data.LAORepository;
import com.github.dedis.popstellar.model.event.EventState;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import java.util.Optional;

/**
 * Roll Call messages handler class
 */
public class RollCallHandler {

  public static final String TAG = RollCallHandler.class.getSimpleName();

  public static final String MESSAGE_ID = "Message ID : ";
  public static final String ROLL_CALL_ID = "Roll Call ID : ";
  public static final String ROLL_CALL_NAME = "Roll Call Name : ";
  public static final String ROLL_CALL_LOCATION = "Location : ";
  public static final String ROLL_CALL_UPDATED_ID = "Updated ID : ";
  public static final String ROLL_CALL_CREATION = "New Roll Call was created ";
  public static final String ROLL_CALL_OPENING = "A Roll Call was opened ";
  public static final String ROLL_CALL_DELETION = "A Roll Call was closed ";

  /**
   * Process a CreateRollCall message.
   *
   * @param laoRepository  the repository to access the LAO of the channel
   * @param channel        the channel on which the message was received
   * @param createRollCall the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleCreateRollCall(LAORepository laoRepository, String channel,
      CreateRollCall createRollCall,
      String messageId) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Log.d(TAG, "handleCreateRollCall: " + channel + " name " + createRollCall.getName());

    RollCall rollCall = new RollCall(createRollCall.getId());
    rollCall.setCreation(createRollCall.getCreation());
    rollCall.setState(EventState.CREATED);
    rollCall.setStart(createRollCall.getProposedStart());
    rollCall.setEnd(createRollCall.getProposedEnd());
    rollCall.setName(createRollCall.getName());
    rollCall.setLocation(createRollCall.getLocation());

    rollCall.setLocation(createRollCall.getLocation());
    rollCall.setDescription(createRollCall.getDescription().orElse(""));

    lao.updateRollCall(rollCall.getId(), rollCall);

    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(ROLL_CALL_CREATION);
    message.setDescription(
        ROLL_CALL_NAME + rollCall.getName() + "\n" + ROLL_CALL_ID + rollCall.getId() + "\n"
            + ROLL_CALL_LOCATION + rollCall.getLocation() + "\n" + MESSAGE_ID + messageId);

    lao.updateWitnessMessage(messageId, message);

    return false;
  }

  /**
   * Process an OpenRollCall message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param openRollCall  the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleOpenRollCall(LAORepository laoRepository, String channel,
      OpenRollCall openRollCall, String messageId) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Log.d(TAG, "handleOpenRollCall: " + channel);
    Log.d(TAG, openRollCall.getOpens());

    String updateId = openRollCall.getUpdateId();
    String opens = openRollCall.getOpens();

    Optional<RollCall> rollCallOptional = lao.getRollCall(opens);
    if (!rollCallOptional.isPresent()) {
      return true;
    }

    RollCall rollCall = rollCallOptional.get();
    rollCall.setStart(openRollCall.getOpenedAt());
    rollCall.setState(EventState.OPENED);
    // We might be opening a closed one
    rollCall.setEnd(0);
    rollCall.setId(updateId);

    lao.updateRollCall(opens, rollCall);

    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(ROLL_CALL_OPENING);
    message.setDescription(
        ROLL_CALL_NAME + rollCall.getName() + "\n" + ROLL_CALL_UPDATED_ID + rollCall.getId() + "\n"
            + MESSAGE_ID + messageId);
    lao.updateWitnessMessage(messageId, message);
    return false;
  }

  /**
   * Process a CloseRollCall message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param closeRollCall the message that was received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleCloseRollCall(LAORepository laoRepository, String channel,
      CloseRollCall closeRollCall,
      String messageId) {
    Lao lao = laoRepository.getLaoByChannel(channel);
    Log.d(TAG, "handleCloseRollCall: " + channel);

    String updateId = closeRollCall.getUpdateId();
    String closes = closeRollCall.getCloses();

    Optional<RollCall> rollCallOptional = lao.getRollCall(closes);
    if (!rollCallOptional.isPresent()) {
      return true;
    }

    RollCall rollCall = rollCallOptional.get();
    rollCall.setEnd(closeRollCall.getClosedAt());
    rollCall.setId(updateId);
    rollCall.getAttendees().addAll(closeRollCall.getAttendees());
    rollCall.setState(EventState.CLOSED);

    lao.updateRollCall(closes, rollCall);

    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(ROLL_CALL_DELETION);
    message.setDescription(
        ROLL_CALL_NAME + rollCall.getName() + "\n" + ROLL_CALL_UPDATED_ID + rollCall.getId() + "\n"
            + MESSAGE_ID + messageId);
    lao.updateWitnessMessage(messageId, message);
    return false;
  }
}
