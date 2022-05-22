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

    const mtgMsg = msg.messageData as CreateMeeting;

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

    const lao = getLaoById(msg.laoId.valueOf());
    if (!lao) {
      console.warn(makeErr(`no known lao with id '${msg.laoId}'`));
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
      console.warn(makeErr(`no known meeting with id ${mtgMsg.id}`));
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
