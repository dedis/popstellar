package com.github.dedis.popstellar.utility.handler.data;

import android.annotation.SuppressLint;
import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.RollCallRepository;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;

import java.util.Set;

import javax.inject.Inject;

/** Roll Call messages handler class */
public final class RollCallHandler {

  public static final String TAG = RollCallHandler.class.getSimpleName();

  private static final String ROLL_CALL_NAME = "Roll Call Name : ";
  private static final String MESSAGE_ID = "Message ID : ";

  private final LAORepository laoRepo;
  private final RollCallRepository rollCallRepo;

  @Inject
  public RollCallHandler(LAORepository laoRepo, RollCallRepository rollCallRepo) {
    this.laoRepo = laoRepo;
    this.rollCallRepo = rollCallRepo;
  }

  /**
   * Process a CreateRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param createRollCall the message that was received
   */
  public void handleCreateRollCall(HandlerContext context, CreateRollCall createRollCall)
      throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, "handleCreateRollCall: " + channel + " name " + createRollCall.getName());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    RollCallBuilder builder = new RollCallBuilder();
    builder
        .setId(createRollCall.getId())
        .setPersistentId(createRollCall.getId())
        .setCreation(createRollCall.getCreation())
        .setState(EventState.CREATED)
        .setStart(createRollCall.getProposedStart())
        .setEnd(createRollCall.getProposedEnd())
        .setName(createRollCall.getName())
        .setLocation(createRollCall.getLocation())
        .setDescription(createRollCall.getDescription().orElse(""))
        .setEmptyAttendees();

    RollCall rollCall = builder.build();
    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageId, createRollCallWitnessMessage(messageId, rollCall));

    rollCallRepo.updateRollCall(laoView.getId(), rollCall);
    laoRepo.updateLao(lao);
  }

  /**
   * Process an OpenRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param openRollCall the message that was received
   */
  public void handleOpenRollCall(HandlerContext context, OpenRollCall openRollCall)
      throws UnknownLaoException, UnknownRollCallException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, "handleOpenRollCall: " + channel + " msg=" + openRollCall);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    String updateId = openRollCall.getUpdateId();
    String opens = openRollCall.getOpens();

    RollCall existingRollCall = rollCallRepo.getRollCallWithId(laoView.getId(), opens);
    RollCallBuilder builder = new RollCallBuilder();
    builder
        .setId(updateId)
        .setPersistentId(existingRollCall.getPersistentId())
        .setCreation(existingRollCall.getCreation())
        .setState(EventState.OPENED)
        .setStart(openRollCall.getOpenedAt())
        .setEnd(existingRollCall.getEnd())
        .setName(existingRollCall.getName())
        .setLocation(existingRollCall.getLocation())
        .setDescription(existingRollCall.getDescription())
        .setEmptyAttendees();

    RollCall rollCall = builder.build();
    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageId, openRollCallWitnessMessage(messageId, rollCall));

    rollCallRepo.updateRollCall(laoView.getId(), rollCall);
    laoRepo.updateLao(lao);
  }

  /**
   * Process a CloseRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param closeRollCall the message that was received
   */
  @SuppressLint("CheckResult")
  public void handleCloseRollCall(HandlerContext context, CloseRollCall closeRollCall)
      throws UnknownLaoException, UnknownRollCallException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, "handleCloseRollCall: " + channel);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    String updateId = closeRollCall.getUpdateId();
    String closes = closeRollCall.getCloses();

    RollCall existingRollCall = rollCallRepo.getRollCallWithId(laoView.getId(), closes);
    Set<PublicKey> currentAttendees = existingRollCall.getAttendees();
    currentAttendees.addAll(closeRollCall.getAttendees());

    RollCallBuilder builder = new RollCallBuilder();
    builder
        .setId(updateId)
        .setPersistentId(existingRollCall.getPersistentId())
        .setCreation(existingRollCall.getCreation())
        .setState(EventState.CLOSED)
        .setStart(existingRollCall.getStart())
        .setName(existingRollCall.getName())
        .setLocation(existingRollCall.getLocation())
        .setDescription(existingRollCall.getDescription())
        .setAttendees(currentAttendees)
        .setEnd(closeRollCall.getClosedAt());

    RollCall rollCall = builder.build();
    rollCallRepo.updateRollCall(laoView.getId(), rollCall);

    Lao lao = laoView.createLaoCopy();
    lao.updateTransactionHashMap(closeRollCall.getAttendees());
    lao.updateWitnessMessage(messageId, closeRollCallWitnessMessage(messageId, rollCall));

    // Subscribe to the social media channels
    // (this is not the expected behavior as users should be able to choose who to subscribe to. But
    // as this part is not implemented, currently, it subscribes to everyone)
    rollCall
        .getAttendees()
        .forEach(
            token ->
                context
                    .getMessageSender()
                    .subscribe(channel.subChannel("social").subChannel(token.getEncoded()))
                    .subscribe(
                        () -> Log.d(TAG, "subscription a success"),
                        error -> Log.d(TAG, "subscription error")));

    laoRepo.updateLao(lao);
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
