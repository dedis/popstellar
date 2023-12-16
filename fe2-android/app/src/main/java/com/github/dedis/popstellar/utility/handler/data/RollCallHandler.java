package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.rollcall.*;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.EventState;
import com.github.dedis.popstellar.model.objects.event.RollCallBuilder;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.security.PublicKey;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.UnknownRollCallException;
import java.util.*;
import javax.inject.Inject;
import timber.log.Timber;

/** Roll Call messages handler class */
public final class RollCallHandler {

  private static final String TAG = RollCallHandler.class.getSimpleName();

  private static final String MNEMONIC_STRING = "Mnemonic identifier :\n";
  private static final String LOCATION_STRING = "Location :\n";
  private static final String DESCRIPTION_STRING = "Description :\n";

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
      throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("handleCreateRollCall: channel: %s, name: %s", channel, createRollCall.name);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    RollCallBuilder builder = new RollCallBuilder();
    builder
        .setId(createRollCall.id)
        .setPersistentId(createRollCall.id)
        .setCreation(createRollCall.creation)
        .setState(EventState.CREATED)
        .setStart(createRollCall.proposedStart)
        .setEnd(createRollCall.proposedEnd)
        .setName(createRollCall.name)
        .setLocation(createRollCall.location)
        .setDescription(createRollCall.getDescription().orElse(""))
        .setEmptyAttendees();

    String laoId = laoView.getId();
    RollCall rollCall = builder.build();

    witnessingRepo.addWitnessMessage(laoId, createRollCallWitnessMessage(messageId, rollCall));
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall);
    } else {
      // Update the repo with the created rollcall when the witness policy is satisfied
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, laoId, rollCall));
    }
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

    Timber.tag(TAG).d("handleOpenRollCall: channel: %s, msg: %s", channel, openRollCall);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    String updateId = openRollCall.updateId;
    String opens = openRollCall.opens;

    RollCall existingRollCall = rollCallRepo.getRollCallWithId(laoView.getId(), opens);
    RollCallBuilder builder = new RollCallBuilder();
    builder
        .setId(updateId)
        .setPersistentId(existingRollCall.persistentId)
        .setCreation(existingRollCall.creation)
        .setState(EventState.OPENED)
        .setStart(openRollCall.openedAt)
        .setEnd(existingRollCall.end)
        .setName(existingRollCall.getName())
        .setLocation(existingRollCall.location)
        .setDescription(existingRollCall.description)
        .setEmptyAttendees();

    String laoId = laoView.getId();
    RollCall rollCall = builder.build();

    witnessingRepo.addWitnessMessage(laoId, openRollCallWitnessMessage(messageId, rollCall));
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall);
    } else {
      // Update the repo with the created rollcall when the witness policy is satisfied
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, laoId, rollCall));
    }
  }

  /**
   * Process a CloseRollCall message.
   *
   * @param context the HandlerContext of the message
   * @param closeRollCall the message that was received
   */
  public void handleCloseRollCall(HandlerContext context, CloseRollCall closeRollCall)
      throws UnknownLaoException, UnknownRollCallException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("handleCloseRollCall: channel: %s", channel);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    String updateId = closeRollCall.updateId;
    String closes = closeRollCall.closes;

    RollCall existingRollCall = rollCallRepo.getRollCallWithId(laoView.getId(), closes);
    Set<PublicKey> currentAttendees = existingRollCall.attendees;
    currentAttendees.addAll(closeRollCall.getAttendees());

    RollCallBuilder builder = new RollCallBuilder();
    builder
        .setId(updateId)
        .setPersistentId(existingRollCall.persistentId)
        .setCreation(existingRollCall.creation)
        .setState(EventState.CLOSED)
        .setStart(existingRollCall.start)
        .setName(existingRollCall.getName())
        .setLocation(existingRollCall.location)
        .setDescription(existingRollCall.description)
        .setAttendees(currentAttendees)
        .setEnd(closeRollCall.closedAt);

    String laoId = laoView.getId();
    RollCall rollCall = builder.build();

    witnessingRepo.addWitnessMessage(laoId, closeRollCallWitnessMessage(messageId, rollCall));
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall);
    } else {
      // Update the repo with the closed rollcall when the witness policy is
      // satisfied and apply a specific routine
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, laoId, rollCall));
    }

    // Subscribe to the social media channels
    // (this is not the expected behavior as users should be able to choose who to subscribe to. But
    // as this part is not implemented, currently, it subscribes to everyone)
    rollCall.attendees
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

  public static void addRollCallRoutine(
      RollCallRepository rollCallRepo,
      DigitalCashRepository digitalCashRepo,
      String laoId,
      RollCall rollCall) {
    rollCallRepo.updateRollCall(laoId, rollCall);
    if (rollCall.isClosed()) {
      digitalCashRepo.initializeDigitalCash(laoId, new ArrayList<>(rollCall.attendees));
    }
  }

  public static WitnessMessage createRollCallWitnessMessage(
      MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.title = String.format(
        "The Roll Call %s was created at %s",
        rollCall.getName(), new Date(rollCall.creation * 1000));
    message.description = MNEMONIC_STRING
        + ActivityUtils.generateMnemonicWordFromBase64(rollCall.persistentId, 2)
        + "\n\n"
        + LOCATION_STRING
        + rollCall.location
        + "\n\n"
        + (rollCall.description.isEmpty()
            ? ""
            : (DESCRIPTION_STRING + rollCall.description + "\n\n"))
        + "Opens at :\n"
        + new Date(rollCall.getStartTimestampInMillis())
        + "\n\n"
        + "Closes at :\n"
        + new Date(rollCall.getEndTimestampInMillis());

    return message;
  }

  public static WitnessMessage openRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.title = String.format(
        "The Roll Call %s was opened at %s",
        rollCall.getName(), new Date(rollCall.getStartTimestampInMillis()));
    message.description = MNEMONIC_STRING
        + ActivityUtils.generateMnemonicWordFromBase64(rollCall.persistentId, 2)
        + "\n\n"
        + LOCATION_STRING
        + rollCall.location
        + "\n\n"
        + (rollCall.description.isEmpty()
            ? ""
            : (DESCRIPTION_STRING + rollCall.description + "\n\n"))
        + "Closes at :\n"
        + new Date(rollCall.getEndTimestampInMillis());

    return message;
  }

  public static WitnessMessage closeRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.title = String.format(
        "The Roll Call %s was closed at %s",
        rollCall.getName(), new Date(rollCall.getEndTimestampInMillis()));
    message.description = MNEMONIC_STRING
        + ActivityUtils.generateMnemonicWordFromBase64(rollCall.persistentId, 2)
        + "\n\n"
        + LOCATION_STRING
        + rollCall.location
        + "\n\n"
        + (rollCall.description.isEmpty()
            ? ""
            : (DESCRIPTION_STRING + rollCall.description + "\n\n"))
        + formatAttendees(rollCall.attendees);

    return message;
  }

  private static String formatAttendees(Set<PublicKey> attendees) {
    StringBuilder stringBuilder = new StringBuilder("Attendees :");
    int index = 1;
    for (PublicKey attendee : attendees) {
      stringBuilder.append("\n").append(index).append(") ").append(attendee.getEncoded());
      ++index;
    }
    return stringBuilder.toString();
  }
}
