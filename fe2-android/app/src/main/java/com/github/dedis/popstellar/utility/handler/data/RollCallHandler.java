package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.utility.error.*;

import java.util.ArrayList;
import java.util.Set;

import javax.inject.Inject;

import timber.log.Timber;

/** Roll Call messages handler class */
public final class RollCallHandler {

  public static final String TAG = RollCallHandler.class.getSimpleName();

  private static final String ROLL_CALL_NAME = "Roll Call Name : ";
  private static final String MESSAGE_ID = "Message ID : ";

  private final LAORepository laoRepo;
  private final RollCallRepository rollCallRepo;
  private final DigitalCashRepository digitalCashRepo;
  private final WitnessingRepository witnessingRepo;

  @Inject
  public RollCallHandler(
      LAORepository laoRepo,
      RollCallRepository rollCallRepo,
      DigitalCashRepository digitalCashRepo,
      WitnessingRepository witnessingRepo) {
    this.laoRepo = laoRepo;
    this.rollCallRepo = rollCallRepo;
    this.digitalCashRepo = digitalCashRepo;
    this.witnessingRepo = witnessingRepo;
  }

  /**
   * Process a CreateRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param createRollCall the message that was received
   */
  public void handleCreateRollCall(HandlerContext context, CreateRollCall createRollCall)
      throws UnknownLaoException, UnknownWitnessMessageException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG)
        .d("handleCreateRollCall: channel: %s, name: %s", channel, createRollCall.getName());
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

    String laoId = laoView.getId();
    RollCall rollCall = builder.build();

    witnessingRepo.addWitnessMessage(laoId, createRollCallWitnessMessage(messageId, rollCall));

    // Update the repo with the created rollcall when the witness policy is satisfied
    witnessingRepo.performActionWhenWitnessThresholdReached(
        laoId, messageId, () -> rollCallRepo.updateRollCall(laoId, rollCall));
  }

  /**
   * Process an OpenRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param openRollCall the message that was received
   */
  public void handleOpenRollCall(HandlerContext context, OpenRollCall openRollCall)
      throws UnknownLaoException, UnknownRollCallException, UnknownWitnessMessageException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("handleOpenRollCall: channel: %s, msg: %s", channel, openRollCall);
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

    String laoId = laoView.getId();
    RollCall rollCall = builder.build();

    witnessingRepo.addWitnessMessage(laoId, openRollCallWitnessMessage(messageId, rollCall));

    // Update the repo with the opened rollcall when the witness policy is satisfied
    witnessingRepo.performActionWhenWitnessThresholdReached(
        laoId, messageId, () -> rollCallRepo.updateRollCall(laoId, rollCall));
  }

  /**
   * Process a CloseRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param closeRollCall the message that was received
   */
  public void handleCloseRollCall(HandlerContext context, CloseRollCall closeRollCall)
      throws UnknownLaoException, UnknownRollCallException, UnknownWitnessMessageException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("handleCloseRollCall: channel: %s", channel);
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

    String laoId = laoView.getId();
    RollCall rollCall = builder.build();

    witnessingRepo.addWitnessMessage(laoId, closeRollCallWitnessMessage(messageId, rollCall));

    // Update the repo with the closed rollcall when the witness policy is satisfied and apply a
    // specific routine
    witnessingRepo.performActionWhenWitnessThresholdReached(
        laoId, messageId, () -> closeRollCallRoutine(laoId, rollCall, context, channel));
  }

  private void closeRollCallRoutine(
      String laoId, RollCall rollCall, HandlerContext context, Channel channel) {
    rollCallRepo.updateRollCall(laoId, rollCall);

    digitalCashRepo.initializeDigitalCash(laoId, new ArrayList<>(rollCall.getAttendees()));

    // Subscribe to the social media channels
    // (this is not the expected behavior as users should be able to choose who to subscribe to. But
    // as this part is not implemented, currently, it subscribes to everyone)
    rollCall
        .getAttendees()
        .forEach(
            token ->
                rollCallRepo.addDisposable(
                    context
                        .getMessageSender()
                        .subscribe(channel.subChannel("social").subChannel(token.getEncoded()))
                        .subscribe(
                            () -> Timber.tag(TAG).d("subscription a success"),
                            error -> Timber.tag(TAG).e(error, "subscription error"))));

    // Subscribe to reactions
    rollCallRepo.addDisposable(
        context
            .getMessageSender()
            .subscribe(channel.subChannel("social").subChannel("reactions"))
            .subscribe(
                () -> Timber.tag(TAG).d("subscription a success"),
                error -> Timber.tag(TAG).e(error, "subscription error")));
  }

  public static WitnessMessage createRollCallWitnessMessage(
      MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("New Roll Call was created");
    message.setDescription(
        ROLL_CALL_NAME
            + "\n"
            + rollCall.getName()
            + "\n\n"
            + "Roll Call ID : "
            + "\n"
            + rollCall.getId()
            + "\n\n"
            + "Location : "
            + "\n"
            + rollCall.getLocation()
            + "\n\n"
            + MESSAGE_ID
            + "\n"
            + messageId.getEncoded());

    return message;
  }

  public static WitnessMessage openRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("Roll Call was opened");
    message.setDescription(
        ROLL_CALL_NAME
            + "\n"
            + rollCall.getName()
            + "\n\n"
            + "Updated Roll Call ID :"
            + "\n"
            + rollCall.getId()
            + "\n\n"
            + MESSAGE_ID
            + "\n"
            + messageId.getEncoded());

    return message;
  }

  public static WitnessMessage closeRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("Roll Call was closed");
    message.setDescription(
        ROLL_CALL_NAME
            + "\n"
            + rollCall.getName()
            + "\n\n"
            + "Updated Roll Call ID : "
            + rollCall.getId()
            + "\n\n"
            + MESSAGE_ID
            + "\n"
            + messageId.getEncoded());

    return message;
  }
}
