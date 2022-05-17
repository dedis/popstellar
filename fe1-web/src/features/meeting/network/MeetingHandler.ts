import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { hasWitnessSignatureQuorum } from 'core/network/validation/Checker';
import { dispatch } from 'core/redux';

import { MeetingConfiguration } from '../interface';
import { Meeting } from '../objects';
import { CreateMeeting, StateMeeting } from './messages';

/**
 * Handles all meeting related messages that are received.
 */

/**
 * Handles a MeetingCreate message by creating a meeting in the current Lao.
 *
 * @param addEvent - An action creator to add a lao event
 */
export const handleMeetingCreateMessage =
  (addEvent: MeetingConfiguration['addEvent']) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.MEETING ||
      msg.messageData.action !== ActionType.CREATE
    ) {
      console.warn('handleMeetingCreateMessage was called to process an unsupported message', msg);
      return false;
    }

    const mtgMsg = msg.messageData as CreateMeeting;
    mtgMsg.validate(msg.laoId);

    const meeting = new Meeting({
      id: mtgMsg.id,
      name: mtgMsg.name,
      location: mtgMsg.location,
      creation: mtgMsg.creation,
      start: mtgMsg.start,
      end: mtgMsg.end ? mtgMsg.end : undefined,
      extra: mtgMsg.extra ? { ...mtgMsg.extra } : {},
    });

    dispatch(addEvent(msg.laoId, meeting.toState()));
    return true;
  };

/**
 * Handles a MeetingState message by updating the state of the meeting.
 *
 * @param getLaoById - A function to get laos by their id
 * @param getEventById - A function to get events by their id
 * @param getEventById - An action creator to update lao events
 */
export const handleMeetingStateMessage =
  (
    getLaoById: MeetingConfiguration['getLaoById'],
    getEventById: MeetingConfiguration['getEventById'],
    updateEvent: MeetingConfiguration['updateEvent'],
  ) =>
  (msg: ProcessableMessage): boolean => {
    if (
      msg.messageData.object !== ObjectType.MEETING ||
      msg.messageData.action !== ActionType.STATE
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

    const mtgMsg = msg.messageData as StateMeeting;
    if (!hasWitnessSignatureQuorum(mtgMsg.modification_signatures, lao)) {
      console.warn(makeErr('witness quorum was not reached'));
      return false;
    }

    // FIXME: use meeting reducer
    const oldMeeting = getEventById(mtgMsg.id) as Meeting;
    if (!oldMeeting) {
      console.warn(makeErr("no known meeting matching the 'id' field"));
      return false;
    }

    const meeting = new Meeting({
      ...oldMeeting,
      lastModified: mtgMsg.last_modified,
      location: mtgMsg.location,
      start: mtgMsg.start,
      end: mtgMsg.end,
      extra: {
        ...oldMeeting.extra,
        ...mtgMsg.extra,
      },
    });

    dispatch(updateEvent(msg.laoId, meeting.toState()));
    return true;
  };
