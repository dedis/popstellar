import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { hasWitnessSignatureQuorum } from 'core/network/validation/Checker';
import { Hash } from 'core/objects';
import { dispatch, getStore } from 'core/redux';
import { selectEventById } from 'features/events/network/EventHandlerUtils';
import { addEvent, updateEvent } from 'features/events/reducer';
import { selectLaosMap } from 'features/lao/reducer';

import { Meeting } from '../objects';
import { CreateMeeting, StateMeeting } from './messages';

/**
 * Handles all meeting related messages that are received.
 */

const getLao = (laoId: Hash | string) => selectLaosMap(getStore().getState())[laoId.valueOf()];

/**
 * Handles a MeetingCreate message by creating a meeting in the current Lao.
 *
 * @param msg - The extended message for creating a meeting
 */
export function handleMeetingCreateMessage(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.MEETING ||
    msg.messageData.action !== ActionType.CREATE
  ) {
    console.warn('handleMeetingCreateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `meeting/create was not processed: ${err}`;

  const lao = getLao(msg.laoId);
  if (!lao) {
    console.warn(makeErr(`LAO ${msg.laoId} does not exist`));
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
}

/**
 * Handles a MeetingState message by updating the state of the meeting.
 *
 * @param msg - The extended message for getting the meeting's state
 */
export function handleMeetingStateMessage(msg: ProcessableMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.MEETING ||
    msg.messageData.action !== ActionType.STATE
  ) {
    console.warn('handleMeetingStateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `meeting/state was not processed: ${err}`;

  const lao = getLao(msg.laoId);
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
  const storeState = getStore().getState();
  const oldMeeting = selectEventById(storeState, mtgMsg.id) as Meeting;
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
}
