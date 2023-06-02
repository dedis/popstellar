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

    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall);
    } else {
      witnessingRepo.addWitnessMessage(laoId, createRollCallWitnessMessage(messageId, rollCall));
      // Update the repo with the created rollcall when the witness policy is satisfied
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, rollCall));
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

    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall);
    } else {
      witnessingRepo.addWitnessMessage(laoId, openRollCallWitnessMessage(messageId, rollCall));
      // Update the repo with the created rollcall when the witness policy is satisfied
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, rollCall));
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

    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addRollCallRoutine(rollCallRepo, digitalCashRepo, laoId, rollCall);
    } else {
      witnessingRepo.addWitnessMessage(laoId, closeRollCallWitnessMessage(messageId, rollCall));
      // Update the repo with the closed rollcall when the witness policy is
      // satisfied and apply a specific routine
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, rollCall));
    }

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

  public static void addRollCallRoutine(
      RollCallRepository rollCallRepo,
      DigitalCashRepository digitalCashRepo,
      String laoId,
      RollCall rollCall) {
    rollCallRepo.updateRollCall(laoId, rollCall);
    if (rollCall.isClosed()) {
      digitalCashRepo.initializeDigitalCash(laoId, new ArrayList<>(rollCall.getAttendees()));
    }
  }

  public static WitnessMessage createRollCallWitnessMessage(
      MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(
        String.format(
            "The Roll Call %s was created at %s",
            rollCall.getName(), new Date(rollCall.getCreation() * 1000)));
    message.setDescription(
        MNEMONIC_STRING
            + ActivityUtils.generateMnemonicWordFromBase64(rollCall.getPersistentId(), 2)
            + "\n\n"
            + LOCATION_STRING
            + rollCall.getLocation()
            + "\n\n"
            + (rollCall.getDescription().isEmpty()
                ? ""
                : (DESCRIPTION_STRING + rollCall.getDescription() + "\n\n"))
            + "Opens at :\n"
            + new Date(rollCall.getStartTimestampInMillis())
            + "\n\n"
            + "Closes at :\n"
            + new Date(rollCall.getEndTimestampInMillis()));

    return message;
  }

  public static WitnessMessage openRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(
        String.format(
            "The Roll Call %s was opened at %s",
            rollCall.getName(), new Date(rollCall.getStartTimestampInMillis())));
    message.setDescription(
        MNEMONIC_STRING
            + ActivityUtils.generateMnemonicWordFromBase64(rollCall.getPersistentId(), 2)
            + "\n\n"
            + LOCATION_STRING
            + rollCall.getLocation()
            + "\n\n"
            + (rollCall.getDescription().isEmpty()
                ? ""
                : (DESCRIPTION_STRING + rollCall.getDescription() + "\n\n"))
            + "Closes at :\n"
            + new Date(rollCall.getEndTimestampInMillis()));

    return message;
  }

  public static WitnessMessage closeRollCallWitnessMessage(MessageID messageId, RollCall rollCall) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(
        String.format(
            "The Roll Call %s was closed at %s",
            rollCall.getName(), new Date(rollCall.getEndTimestampInMillis())));
    message.setDescription(
        MNEMONIC_STRING
            + ActivityUtils.generateMnemonicWordFromBase64(rollCall.getPersistentId(), 2)
            + "\n\n"
            + LOCATION_STRING
            + rollCall.getLocation()
            + "\n\n"
            + (rollCall.getDescription().isEmpty()
                ? ""
                : (DESCRIPTION_STRING + rollCall.getDescription() + "\n\n"))
            + formatAttendees(rollCall.getAttendees()));

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
