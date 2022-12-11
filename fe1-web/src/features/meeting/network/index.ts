import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import { MeetingConfiguration } from '../interface';
import { Meeting } from '../objects';
import { addMeeting, getMeetingById, updateMeeting } from '../reducer';
import { handleMeetingCreateMessage, handleMeetingStateMessage } from './MeetingHandler';
import { CreateMeeting, StateMeeting } from './messages';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param configuration - The configuration object for the meeting feature
 */
export const configureNetwork = (configuration: MeetingConfiguration) => {
  // getMeetingById bound to the global state
  const boundGetMeetingById = (meetingId: Hash) => getMeetingById(meetingId, getStore().getState());

  const addMeetingEvent = (laoId: Hash, meeting: Meeting) => {
    const meetingState = meeting.toState();

    dispatch(addMeeting(meetingState));
    dispatch(
      configuration.addEvent(laoId, {
        eventType: Meeting.EVENT_TYPE,
        id: meetingState.id,
        start: meeting.start.valueOf(),
        end: meeting.end?.valueOf(),
      }),
    );
  };

  const updateMeetingEvent = (meeting: Meeting) => {
    const meetingState = meeting.toState();

    dispatch(updateMeeting(meetingState));
    dispatch(
      configuration.updateEvent({
        eventType: Meeting.EVENT_TYPE,
        id: meetingState.id,
        start: meetingState.start,
        end: meetingState.end,
      }),
    );
  };

  configuration.messageRegistry.add(
    ObjectType.MEETING,
    ActionType.CREATE,
    handleMeetingCreateMessage(addMeetingEvent),
    CreateMeeting.fromJson,
  );

  configuration.messageRegistry.add(
    ObjectType.MEETING,
    ActionType.STATE,
    handleMeetingStateMessage(configuration.getLaoById, boundGetMeetingById, updateMeetingEvent),
    StateMeeting.fromJson,
  );
};
