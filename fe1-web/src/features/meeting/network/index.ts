import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';
import { dispatch, getStore } from 'core/redux';

import { MeetingConfiguration } from '../interface';
import { Meeting, MeetingState } from '../objects';
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
  const boundGetMeetingById = (meetingId: Hash | string) =>
    getMeetingById(meetingId, getStore().getState());

  const addMeetingEvent = (laoId: Hash | string, meetingState: MeetingState) => {
    dispatch(configuration.addEvent(laoId, Meeting.EVENT_TYPE, meetingState.id));
    dispatch(addMeeting(meetingState));
  };

  const updateMeetingEvent = (laoId: Hash | string, meetingState: MeetingState) => {
    dispatch(configuration.addEvent(laoId, Meeting.EVENT_TYPE, meetingState.id));
    dispatch(updateMeeting(meetingState));
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
