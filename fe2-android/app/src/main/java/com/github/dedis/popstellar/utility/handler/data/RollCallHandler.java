package com.github.dedis.popstellar.utility.handler.data;

import android.annotation.SuppressLint;
import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PoPToken;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.utility.error.*;
import com.github.dedis.popstellar.utility.error.keys.InvalidPoPTokenException;
import com.github.dedis.popstellar.utility.error.keys.KeyException;

import java.util.Optional;

/** Roll Call messages handler class */
public final class RollCallHandler {

  public static final String TAG = RollCallHandler.class.getSimpleName();

  private static final String ROLL_CALL_NAME = "Roll Call Name : ";
  private static final String MESSAGE_ID = "Message ID : ";

  private RollCallHandler() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Process a CreateRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param createRollCall the message that was received
   */
  public static void handleCreateRollCall(HandlerContext context, CreateRollCall createRollCall)
      throws UnknownLaoException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, "handleCreateRollCall: " + channel + " name " + createRollCall.getName());
    LaoView laoView = laoRepository.getLaoViewByChannel(channel);

    RollCall rollCall = new RollCall(createRollCall.getId());
    rollCall.setCreation(createRollCall.getCreation());
    rollCall.setState(EventState.CREATED);
    rollCall.setStart(createRollCall.getProposedStart());
    rollCall.setEnd(createRollCall.getProposedEnd());
    rollCall.setName(createRollCall.getName());
    rollCall.setLocation(createRollCall.getLocation());
    rollCall.setLocation(createRollCall.getLocation());
    rollCall.setDescription(createRollCall.getDescription().orElse(""));

    Lao lao = laoView.createLaoCopy();
    lao.updateRollCall(rollCall.getId(), rollCall);
    lao.updateWitnessMessage(messageId, createRollCallWitnessMessage(messageId, rollCall));

    laoRepository.updateLao(lao);
  }

  /**
   * Process an OpenRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param openRollCall the message that was received
   */
  public static void handleOpenRollCall(HandlerContext context, OpenRollCall openRollCall)
      throws DataHandlingException, UnknownLaoException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, "handleOpenRollCall: " + channel + " msg=" + openRollCall);
    LaoView laoView = laoRepository.getLaoViewByChannel(channel);

    String updateId = openRollCall.getUpdateId();
    String opens = openRollCall.getOpens();

    Optional<RollCall> rollCallOptional = laoView.getRollCall(opens);
    if (!rollCallOptional.isPresent()) {
      Log.w(TAG, "Cannot find roll call to open : " + opens);
      throw new InvalidDataException(openRollCall, "open id", opens);
    }

    RollCall rollCall = rollCallOptional.get();
    rollCall.setStart(openRollCall.getOpenedAt());
    rollCall.setState(EventState.OPENED);
    rollCall.setId(updateId);

    Lao lao = laoView.createLaoCopy();
    lao.updateRollCall(opens, rollCall);
    lao.updateWitnessMessage(messageId, openRollCallWitnessMessage(messageId, rollCall));

    laoRepository.updateLao(lao);
  }

  /**
   * Process a CloseRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param closeRollCall the message that was received
   */
  @SuppressLint("CheckResult")
  public static void handleCloseRollCall(HandlerContext context, CloseRollCall closeRollCall)
      throws DataHandlingException, UnknownLaoException {
    LAORepository laoRepository = context.getLaoRepository();
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, "handleCloseRollCall: " + channel);
    LaoView laoView = laoRepository.getLaoViewByChannel(channel);

    String updateId = closeRollCall.getUpdateId();
    String closes = closeRollCall.getCloses();

    Optional<RollCall> rollCallOptional = laoView.getRollCall(closes);
    if (!rollCallOptional.isPresent()) {
      Log.w(TAG, "Cannot find roll call to close : " + closes);
      throw new InvalidDataException(closeRollCall, "close id", closes);
    }

    RollCall rollCall = rollCallOptional.get();
    rollCall.setEnd(closeRollCall.getClosedAt());
    rollCall.setId(updateId);
    rollCall.getAttendees().addAll(closeRollCall.getAttendees());
    rollCall.setState(EventState.CLOSED);

    Lao lao = laoView.createLaoCopy();
    lao.updateRollCall(closes, rollCall);
    lao.updateTransactionHashMap(closeRollCall.getAttendees());
    lao.updateWitnessMessage(messageId, closeRollCallWitnessMessage(messageId, rollCall));

    // Subscribe to the social media channels
    // Subscribe to the digital cash channels
    try {
      PoPToken token = context.getKeyManager().getValidPoPToken(laoView.getId(), rollCall);
      context
          .getMessageSender()
          .subscribe(channel.subChannel("social").subChannel(token.getPublicKey().getEncoded()))
          .subscribe(
              () -> Log.d(TAG, "subscription a success"),
              error -> Log.d(TAG, "subscription error"));

    } catch (InvalidPoPTokenException e) {
      Log.i(TAG, "Received a close roll-call that you did not attend");
    } catch (KeyException e) {
      Log.e(
          TAG,
          "Could not retrieve your PoP Token to subscribe you to your social media channel",
          e);
    }

    laoRepository.updateLao(lao);
  }

  public static WitnessMessage createRollCallWitnessMessage(
      MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("New Roll Call was created");
    message.setDescription(
        ROLL_CALL_NAME
            + rollCall.getName()
            + "\n"
            + "Roll Call ID : "
            + rollCall.getId()
            + "\n"
            + "Location : "
            + rollCall.getLocation()
            + "\n"
            + MESSAGE_ID
            + messageId);

    return message;
  }

  public static WitnessMessage openRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("A Roll Call was opened");
    message.setDescription(
        ROLL_CALL_NAME
            + rollCall.getName()
            + "\n"
            + "Updated ID : "
            + rollCall.getId()
            + "\n"
            + MESSAGE_ID
            + messageId);

    return message;
  }

  public static WitnessMessage closeRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("A Roll Call was closed");
    message.setDescription(
        ROLL_CALL_NAME
            + rollCall.getName()
            + "\n"
            + "Updated ID : "
            + rollCall.getId()
            + "\n"
            + MESSAGE_ID
            + messageId);

    return message;
  }
}
