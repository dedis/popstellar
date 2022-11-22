import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { hasWitnessSignatureQuorum } from 'core/network/validation/Checker';
import { Hash } from 'core/objects';

import { MeetingConfiguration } from '../interface';
import { Meeting } from '../objects';
import { CreateMeeting, StateMeeting } from './messages';

/**
 * Handles all meeting related messages that are received.
 */

/**
 * Handles a MeetingCreate message by creating a meeting in the current Lao.
 * @param addMeeting - A function to add a meeting
 */
export const handleMeetingCreateMessage =
  (addMeeting: (laoId: Hash, meeting: Meeting) => void) =>
  (msg: ProcessableMessage): boolean => {
    const makeErr = (err: string) => `meeting#create was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.MEETING ||
      msg.messageData.action !== ActionType.CREATE
    ) {
      console.warn(
        makeErr(
          `Invalid object or action parameter: ${msg.messageData.object}#${msg.messageData.action}`,
        ),
      );
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
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

    addMeeting(msg.laoId, meeting);
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
    getMeetingById: (meetingId: Hash) => Meeting | undefined,
    updateMeeting: (meeting: Meeting) => void,
  ) =>
  (msg: ProcessableMessage): boolean => {
    const makeErr = (err: string) => `meeting#state was not processed: ${err}`;

    if (
      msg.messageData.object !== ObjectType.MEETING ||
      msg.messageData.action !== ActionType.STATE
    ) {
      console.warn('handleMeetingStateMessage was called to process an unsupported message', msg);
      return false;
    }

    if (!msg.laoId) {
      console.warn(makeErr(`Was not sent on a lao subchannel but rather on '${msg.channel}'`));
      return false;
    }

    const lao = getLaoById(msg.laoId);
    if (!lao) {
      console.warn(makeErr(`no known lao with id '${msg.laoId}'`));
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
      console.warn(makeErr(`no known meeting with id ${meetingMessage.id}`));
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

    updateMeeting(meeting);
    return true;
  };
