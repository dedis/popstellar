package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.MeetingBuilder;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.UnknownWitnessMessageException;

import java.util.ArrayList;
import java.util.Date;

import javax.inject.Inject;

import timber.log.Timber;

public class MeetingHandler {

  private static final String TAG = RollCallHandler.class.getSimpleName();

  private final LAORepository laoRepo;
  private final MeetingRepository meetingRepo;
  private final WitnessingRepository witnessingRepo;

  @Inject
  public MeetingHandler(
      LAORepository laoRepo, MeetingRepository meetingRepo, WitnessingRepository witnessingRepo) {
    this.laoRepo = laoRepo;
    this.meetingRepo = meetingRepo;
    this.witnessingRepo = witnessingRepo;
  }

  /**
   * Process a CreateMeeting message.
   *
   * @param context the HandlerContext of the message
   * @param createMeeting the message that was received
   */
  public void handleCreateMeeting(HandlerContext context, CreateMeeting createMeeting)
      throws UnknownLaoException, UnknownWitnessMessageException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG)
        .d("handleCreateMeeting: channel: %s, name: %s", channel, createMeeting.getName());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    MeetingBuilder builder = new MeetingBuilder();
    builder
        .setId(createMeeting.getId())
        .setCreation(createMeeting.getCreation())
        .setStart(createMeeting.getStart())
        .setEnd(createMeeting.getEnd())
        .setName(createMeeting.getName())
        .setLocation(createMeeting.getLocation().orElse(""))
        .setLastModified(createMeeting.getCreation())
        .setModificationId("")
        .setModificationSignatures(new ArrayList<>());

    String laoId = laoView.getId();
    Meeting meeting = builder.build();

    witnessingRepo.addWitnessMessage(laoId, createMeetingWitnessMessage(messageId, meeting));

    witnessingRepo.performActionWhenWitnessThresholdReached(
        laoId, messageId, () -> meetingRepo.updateMeeting(laoId, meeting));
  }

  public void handleStateMeeting(HandlerContext context, StateMeeting stateMeeting)
      throws UnknownLaoException, UnknownWitnessMessageException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("handleStateMeeting: channel: %s, name: %s", channel, stateMeeting.getName());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    // TODO: modify with the right logic when implementing the state functionality

    MeetingBuilder builder = new MeetingBuilder();
    builder
        .setId(stateMeeting.getId())
        .setCreation(stateMeeting.getCreation())
        .setStart(stateMeeting.getStart())
        .setEnd(stateMeeting.getEnd())
        .setName(stateMeeting.getName())
        .setLocation(stateMeeting.getLocation().orElse(""))
        .setLastModified(stateMeeting.getCreation())
        .setModificationId(stateMeeting.getModificationId())
        .setModificationSignatures(stateMeeting.getModificationSignatures());

    String laoId = laoView.getId();
    Meeting meeting = builder.build();

    witnessingRepo.addWitnessMessage(
        laoView.getId(), stateMeetingWitnessMessage(messageId, meeting));

    witnessingRepo.performActionWhenWitnessThresholdReached(
        laoId, messageId, () -> meetingRepo.updateMeeting(laoId, meeting));
  }

  public static WitnessMessage createMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(
        String.format(
            "The Meeting %s was created at %s",
            meeting.getName(), new Date(meeting.getCreation() * 1000)));
    message.setDescription(
        "Mnemonic identifier :\n"
            + ActivityUtils.generateMnemonicWordFromBase64(meeting.getId(), 2)
            + "\n\n"
            + (meeting.getLocation().isEmpty()
                ? ""
                : ("Location :\n" + meeting.getLocation() + "\n\n"))
            + "Starts at :\n"
            + new Date(meeting.getStartTimestampInMillis())
            + "\n\n"
            + "Finishes at :\n"
            + new Date(meeting.getEndTimestampInMillis()));

    return message;
  }

  public static WitnessMessage stateMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle(
        String.format(
            "The Meeting %s was modified at %s",
            meeting.getName(), new Date(meeting.getLastModified() * 1000)));
    message.setDescription(
        "Mnemonic identifier :\n"
            + ActivityUtils.generateMnemonicWordFromBase64(meeting.getId(), 2)
            + "\n\n"
            + (meeting.getLocation().isEmpty()
                ? ""
                : ("Location :\n" + meeting.getLocation() + "\n\n"))
            + "Starts at :\n"
            + new Date(meeting.getStartTimestampInMillis())
            + "\n\n"
            + "Finishes at :\n"
            + new Date(meeting.getEndTimestampInMillis()));

    return message;
  }
}
