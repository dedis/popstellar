package com.github.dedis.popstellar.utility.handler.data;

import com.github.dedis.popstellar.model.network.method.message.data.meeting.CreateMeeting;
import com.github.dedis.popstellar.model.network.method.message.data.meeting.StateMeeting;
import com.github.dedis.popstellar.repository.LAORepository;
import com.github.dedis.popstellar.repository.MeetingRepository;

import javax.inject.Inject;

public class MeetingHandler {

  private final LAORepository laoRepo;
  private final MeetingRepository meetingRepo;

  @Inject
  public MeetingHandler(LAORepository laoRepo, MeetingRepository meetingRepo) {
    this.laoRepo = laoRepo;
    this.meetingRepo = meetingRepo;
  }

  public void handleCreateMeeting(HandlerContext context, CreateMeeting createMeeting) {}

  public void handleStateMeeting(HandlerContext context, StateMeeting stateMeeting) {}
}
