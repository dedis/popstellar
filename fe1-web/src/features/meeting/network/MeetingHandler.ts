import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { hasWitnessSignatureQuorum } from 'core/network/validation/Checker';
import { Hash } from 'core/objects';

import { MeetingConfiguration } from '../interface';
import { Meeting, MeetingState } from '../objects';
import { CreateMeeting, StateMeeting } from './messages';

/**
 * Handles all meeting related messages that are received.
 */

/**
 * Handles a MeetingCreate message by creating a meeting in the current Lao.
 * @param addMeeting - A function to add a meeting
 */
export const handleMeetingCreateMessage =
  (addMeeting: (laoId: Hash | string, meetingState: MeetingState) => void) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.MEETING ||
      msg.messageData.action !== ActionType.CREATE ||
      !msg.laoId
    ) {
      console.warn('handleMeetingCreateMessage was called to process an unsupported message', msg);
      return false;
    }

    const meetingMessage = msg.messageData as CreateMeeting;

    const meeting = new Meeting({
      id: meetingMessage.id,
      name: meetingMessage.name,
      location: meetingMessage.location,
      creation: meetingMessage.creation,
      start: meetingMessage.start,
      end: meetingMessage.end ? meetingMessage.end : undefined,
      extra: meetingMessage.extra ? { ...meetingMessage.extra } : {},
    });

    addMeeting(msg.laoId, meeting.toState());
    return true;
  };

/**
 * Handles a MeetingState message by updating the state of the meeting.
 *
 * @param getLaoById - A function to get laos by their id
 * @param getMeetingById - A function to get a meeting by its id
 * @param updateMeeting - An function to update a meeting
 */
export const handleMeetingStateMessage =
  (
    getLaoById: MeetingConfiguration['getLaoById'],
    getMeetingById: (meetingId: Hash | string) => Meeting | undefined,
    updateMeeting: (laoId: Hash | string, meetingState: MeetingState) => void,
  ) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.MEETING ||
      msg.messageData.action !== ActionType.STATE ||
      !msg.laoId
    ) {
      console.warn('handleMeetingStateMessage was called to process an unsupported message', msg);
      return false;
    }

    const makeErr = (err: string) => `meeting/state was not processed: ${err}`;

    const lao = getLaoById(msg.laoId.valueOf());
    if (!lao) {
      console.warn(makeErr('no LAO is currently active'));
      return false;
    }

    const meetingMessage = msg.messageData as StateMeeting;
    if (!hasWitnessSignatureQuorum(meetingMessage.modification_signatures, lao)) {
      console.warn(makeErr('witness quorum was not reached'));
      return false;
    }

    // FIXME: use meeting reducer
    const oldMeeting = getMeetingById(meetingMessage.id) as Meeting;
    if (!oldMeeting) {
      console.warn(makeErr("no known meeting matching the 'id' field"));
      return false;
    }

    const meeting = new Meeting({
      ...oldMeeting,
      lastModified: meetingMessage.last_modified,
      location: meetingMessage.location,
      start: meetingMessage.start,
      end: meetingMessage.end,
      extra: {
        ...oldMeeting.extra,
        ...meetingMessage.extra,
      },
    });

    updateMeeting(msg.laoId, meeting.toState());
    return true;
  };
