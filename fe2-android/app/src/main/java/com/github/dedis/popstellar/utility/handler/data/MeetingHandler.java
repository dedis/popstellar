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
        .setCreation(createMeeting.getCreation())
        .setStart(createMeeting.getStart())
        .setEnd(createMeeting.getEnd())
        .setName(createMeeting.getName())
        .setLocation(createMeeting.getLocation().orElse(""))
        .setLastModified(createMeeting.getCreation());

    Meeting meeting = builder.build();
    Lao lao = laoView.createLaoCopy();
    lao.updateWitnessMessage(messageId, createMeetingWitnessMessage(messageId, meeting));

    meetingRepo.addMeeting(laoView.getId(), meeting);
    laoRepo.updateLao(lao);
  }

  public void handleStateMeeting(HandlerContext context, StateMeeting stateMeeting) {
    // TODO in the future
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
    // TODO in the future
    return null;
  }
}
