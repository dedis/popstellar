package com.github.dedis.popstellar.utility.handler.data;

import android.util.Log;

import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.model.objects.*;
import com.github.dedis.popstellar.model.objects.event.MeetingBuilder;
import com.github.dedis.popstellar.model.objects.security.MessageID;
import com.github.dedis.popstellar.model.objects.view.LaoView;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MeetingRepository;
import com.github.dedis.popstellar.utility.error.UnknownLaoException;
import com.github.dedis.popstellar.utility.error.UnknownMeetingException;

import java.util.ArrayList;
import java.util.Arrays;

import javax.inject.Inject;

public class MeetingHandler {

  private static final String TAG = RollCallHandler.class.getSimpleName();

  private static final String MEETING_NAME = "Meeting Name : ";
  private static final String MESSAGE_ID = "Message ID : ";

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

    Log.d(TAG, "handleCreateMeeting: " + channel + " name " + createMeeting.getName());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    MeetingBuilder builder = new MeetingBuilder();
    builder
        .setId(createMeeting.getId())
        .setPersistentId(createMeeting.getId())
        .setCreation(createMeeting.getCreation())
        .setStart(createMeeting.getStart())
        .setEnd(createMeeting.getEnd())
        .setName(createMeeting.getName())
        .setLocation(createMeeting.getLocation())
        .setLastModified(createMeeting.getCreation())
        .setModificationId("")
        .setModificationSignatures(new ArrayList<>());

    Meeting meeting = builder.build();
    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageId, createMeetingWitnessMessage(messageId, meeting));

    meetingRepo.updateMeeting(laoView.getId(), meeting);
    laoRepo.updateLao(lao);
  }

  public void handleStateMeeting(HandlerContext context, StateMeeting stateMeeting)
      throws UnknownLaoException {
    Channel channel = context.getChannel();
    MessageID messageId = context.getMessageId();

    Log.d(TAG, "handleStateMeeting: " + channel + " name " + stateMeeting.getName());
    LaoView laoView = laoRepo.getLaoViewByChannel(channel);

    Meeting existingMeeting;
    try {
      existingMeeting = meetingRepo.getMeetingWithId(laoView.getId(), stateMeeting.getId());
    } catch (UnknownMeetingException e) {
      existingMeeting = null;
    }

    String persistentId =
        existingMeeting == null ? stateMeeting.getId() : existingMeeting.getPersistentId();

    MeetingBuilder builder = new MeetingBuilder();
    builder
        .setId(stateMeeting.getId())
        .setPersistentId(persistentId)
        .setCreation(stateMeeting.getCreation())
        .setStart(stateMeeting.getStart())
        .setEnd(stateMeeting.getEnd())
        .setName(stateMeeting.getName())
        .setLocation(stateMeeting.getLocation())
        .setLastModified(stateMeeting.getLastModified())
        .setModificationId(stateMeeting.getModificationId())
        .setModificationSignatures(stateMeeting.getModificationSignatures());

    Meeting meeting = builder.build();
    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageId, stateMeetingWitnessMessage(messageId, meeting));

    meetingRepo.updateMeeting(laoView.getId(), meeting);
    laoRepo.updateLao(lao);
  }

  public static WitnessMessage createMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("New Meeting was created");
    message.setDescription(
        MEETING_NAME
            + meeting.getName()
            + "\n"
            + "Meeting ID : "
            + meeting.getId()
            + "\n"
            + "Location : "
            + meeting.getLocation()
            + "\n"
            + MESSAGE_ID
            + messageId);

    return message;
  }

  public static WitnessMessage stateMeetingWitnessMessage(MessageID messageId, Meeting meeting) {
    WitnessMessage message = new WitnessMessage(messageId);
    message.setTitle("A meeting was stated");
    message.setDescription(
        MEETING_NAME
            + meeting.getName()
            + "\n"
            + "Meeting ID : "
            + meeting.getId()
            + "\n"
            + "Modification ID : "
            + meeting.getModificationId()
            + "\n"
            + "Modification signatures : "
            + Arrays.toString(meeting.getModificationSignatures().toArray())
            + "\n"
            + MESSAGE_ID
            + messageId);

    return message;
  }
}
