package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.MeetingBuilder;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MeetingRepository;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;

import java.util.ArrayList;

import javax.inject.Inject;

import timber.log.Timber;

public class MeetingHandler {

  private static final String TAG = RollCallHandler.class.getSimpleName();

  private static final String MEETING_NAME = "Meeting Name : ";
  private static final String MESSAGE_ID = "Message ID : ";
  private static final String MEETING_ID = "Meeting ID : ";
  private static final String MODIFICATION_ID = "Modification ID : ";

  private final LAORepository laoRepo;
  private final MeetingRepository meetingRepo;

  @Inject
  public MeetingHandler(LAORepository laoRepo, MeetingRepository meetingRepo) {
    this.laoRepo = laoRepo;
    this.meetingRepo = meetingRepo;
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

    Meeting meeting = builder.build();
    Lao lao = laoView.createLaoCopy();
    lao.addWitnessMessage(createMeetingWitnessMessage(messageId, meeting));

    meetingRepo.updateMeeting(laoView.getId(), meeting);
    laoRepo.updateLao(lao);
  }

  public void handleStateMeeting(HandlerContext context, StateMeeting stateMeeting)
      throws UnknownLaoException {
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

    Meeting meeting = builder.build();
    Lao lao = laoView.createLaoCopy();
    lao.addWitnessMessage(stateMeetingWitnessMessage(messageId, meeting));

    meetingRepo.updateMeeting(laoView.getId(), meeting);
    laoRepo.updateLao(lao);
  }

  public static WitnessMessage createMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("New Meeting was created");
    message.setDescription(
        MEETING_NAME
            + "\n"
            + meeting.getName()
            + "\n\n"
            + MEETING_ID
            + "\n"
            + meeting.getId()
            + "\n\n"
            + MESSAGE_ID
            + "\n"
            + messageId);

    return message;
  }

  public static WitnessMessage stateMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("A meeting was modified");
    message.setDescription(
        MEETING_NAME
            + "\n"
            + meeting.getName()
            + "\n\n"
            + MEETING_ID
            + "\n"
            + meeting.getId()
            + "\n\n"
            + MODIFICATION_ID
            + "\n"
            + meeting.getModificationId()
            + "\n\n"
            + MESSAGE_ID
            + "\n"
            + messageId);

    return message;
  }
}
