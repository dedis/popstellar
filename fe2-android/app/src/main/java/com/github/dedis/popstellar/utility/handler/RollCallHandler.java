package com.github.dedis.popstellar.utility.handler;

import android.util.Log;
import com.github.dedis.popstellar.model.Lao;
import com.github.dedis.popstellar.model.RollCall;
import com.github.dedis.popstellar.model.WitnessMessage;
import com.github.dedis.popstellar.model.data.LAORepository;
import com.github.dedis.popstellar.model.event.EventState;
import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CloseRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.CreateRollCall;
import com.github.dedis.popstellar.model.network.method.message.data.rollcall.OpenRollCall;
import java.util.Objects;
import java.util.Optional;

/**
 * Roll Call messages handler class
 */
public class RollCallHandler {

  public static final String TAG = RollCallHandler.class.getSimpleName();

  private RollCallHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Process a Roll Call message.
   *
   * @param laoRepository the repository to access the LAO of the channel
   * @param channel       the channel on which the message was received
   * @param data          the data of the message received
   * @param messageId     the ID of the message received
   * @return true if the message cannot be processed and false otherwise
   */
  public static boolean handleRollCallMessage(LAORepository laoRepository, String channel,
      Data data,
      String messageId) {
    Log.d(TAG, "handle Roll Call message");

    switch (Objects.requireNonNull(Action.find(data.getAction()))) {
      case CREATE:
        return handleCreateRollCall(laoRepository, channel, (CreateRollCall) data, messageId);
      case OPEN:
      case REOPEN:
        return handleOpenRollCall(laoRepository, channel, (OpenRollCall) data, messageId);
      case CLOSE:
        return handleCloseRollCall(laoRepository, channel, (CloseRollCall) data, messageId);
      default:
        return true;
    }
  }

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

    lao.updateWitnessMessage(messageId, createRollCallWitnessMessage(messageId, rollCall));

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

    lao.updateWitnessMessage(messageId, openRollCallWitnessMessage(messageId, rollCall));
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

    lao.updateWitnessMessage(messageId, closeRollCallWitnessMessage(messageId, rollCall));
    return false;
  }

  public static WitnessMessage createRollCallWitnessMessage(String messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("New Roll Call was created");
    message.setDescription(
        "Roll Call Name : " + rollCall.getName() + "\n" +
            "Roll Call ID : " + rollCall.getId() + "\n" +
            "Location : " + rollCall.getLocation() + "\n" +
            "Message ID : " + messageId);

    return message;
  }

  public static WitnessMessage openRollCallWitnessMessage(String messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("A Roll Call was opened");
    message.setDescription(
        "Roll Call Name : " + rollCall.getName() + "\n" +
            "Updated ID : " + rollCall.getId() + "\n" +
            "Message ID : " + messageId);

    return message;
  }

  public static WitnessMessage closeRollCallWitnessMessage(String messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("A Roll Call was closed");
    message.setDescription(
        "Roll Call Name : " + rollCall.getName() + "\n" +
            "Updated ID : " + rollCall.getId() + "\n" +
            "Message ID : " + messageId);

    return message;
  }
}
