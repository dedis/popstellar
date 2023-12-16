package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.MeetingBuilder;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.*;
import com.github.dedis.popstellar.repository.database.witnessing.PendingEntity;
import com.github.dedis.popstellar.utility.ActivityUtils;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
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
      throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("handleCreateMeeting: channel: %s, name: %s", channel, createMeeting.name);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    MeetingBuilder builder = new MeetingBuilder();
    builder
        .setId(createMeeting.id)
        .setCreation(createMeeting.creation)
        .setStart(createMeeting.start)
        .setEnd(createMeeting.end)
        .setName(createMeeting.name)
        .setLocation(createMeeting.getLocation().orElse(""))
        .setLastModified(createMeeting.creation)
        .setModificationId("")
        .setModificationSignatures(new ArrayList<>());

    String laoId = laoView.getId();
    Meeting meeting = builder.build();

    witnessingRepo.addWitnessMessage(
        laoView.getId(), createMeetingWitnessMessage(messageId, meeting));
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addMeetingRoutine(meetingRepo, laoId, meeting);
    } else {
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, laoId, meeting));
    }
  }

  public void handleStateMeeting(HandlerContext context, StateMeeting stateMeeting)
      throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Timber.tag(TAG).d("handleStateMeeting: channel: %s, name: %s", channel, stateMeeting.name);
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    // TODO: modify with the right logic when implementing the state functionality

    MeetingBuilder builder = new MeetingBuilder();
    builder
        .setId(stateMeeting.id)
        .setCreation(stateMeeting.creation)
        .setStart(stateMeeting.start)
        .setEnd(stateMeeting.end)
        .setName(stateMeeting.name)
        .setLocation(stateMeeting.getLocation().orElse(""))
        .setLastModified(stateMeeting.creation)
        .setModificationId(stateMeeting.modificationId)
        .setModificationSignatures(stateMeeting.getModificationSignatures());

    String laoId = laoView.getId();
    Meeting meeting = builder.build();

    witnessingRepo.addWitnessMessage(
        laoView.getId(), stateMeetingWitnessMessage(messageId, meeting));
    if (witnessingRepo.areWitnessesEmpty(laoId)) {
      addMeetingRoutine(meetingRepo, laoId, meeting);
    } else {
      witnessingRepo.addPendingEntity(new PendingEntity(messageId, laoId, meeting));
    }
  }

  public static void addMeetingRoutine(
      MeetingRepository meetingRepository, String laoId, Meeting meeting) {
    meetingRepository.updateMeeting(laoId, meeting);
  }

  public static WitnessMessage createMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.title = String.format(
        "The Meeting %s was created at %s",
        meeting.getName(), new Date(meeting.creation * 1000));
    message.description = "Mnemonic identifier :\n"
        + ActivityUtils.generateMnemonicWordFromBase64(meeting.id, 2)
        + "\n\n"
        + (meeting.location.isEmpty()
            ? ""
            : ("Location :\n" + meeting.location + "\n\n"))
        + "Starts at :\n"
        + new Date(meeting.getStartTimestampInMillis())
        + "\n\n"
        + "Finishes at :\n"
        + new Date(meeting.getEndTimestampInMillis());

    return message;
  }

  public static WitnessMessage stateMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.title = String.format(
        "The Meeting %s was modified at %s",
        meeting.getName(), new Date(meeting.lastModified * 1000));
    message.description = "Mnemonic identifier :\n"
        + ActivityUtils.generateMnemonicWordFromBase64(meeting.id, 2)
        + "\n\n"
        + (meeting.location.isEmpty()
            ? ""
            : ("Location :\n" + meeting.location + "\n\n"))
        + "Starts at :\n"
        + new Date(meeting.getStartTimestampInMillis())
        + "\n\n"
        + "Finishes at :\n"
        + new Date(meeting.getEndTimestampInMillis());

    return message;
  }
}
