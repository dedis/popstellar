import { ExtendedMessage } from 'model/network/method/message';
import {
  ActionType,
  ObjectType,
  CreateMeeting,
  StateMeeting,
} from 'model/network/method/message/data';
import { Meeting } from 'model/objects';
import { getStore, dispatch, addEvent, updateEvent, makeCurrentLao } from 'store';
import { hasWitnessSignatureQuorum, getEventFromId } from './Utils';

const getCurrentLao = makeCurrentLao();

function handleMeetingCreateMessage(msg: ExtendedMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.MEETING ||
    msg.messageData.action !== ActionType.CREATE
  ) {
    console.warn('handleMeetingCreateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `meeting/create was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
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

  dispatch(addEvent(lao.id, meeting.toState()));
  return true;
}

function handleMeetingStateMessage(msg: ExtendedMessage): boolean {
  if (
    msg.messageData.object !== ObjectType.MEETING ||
    msg.messageData.action !== ActionType.STATE
  ) {
    console.warn('handleMeetingStateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `meeting/state was not processed: ${err}`;

  const mtgMsg = msg.messageData as StateMeeting;
  if (!hasWitnessSignatureQuorum(mtgMsg.modification_signatures)) {
    console.warn(makeErr('witness quorum was not reached'));
    return false;
  }

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const oldMeeting = getEventFromId(storeState, mtgMsg.id) as Meeting;
  if (!oldMeeting) {
    console.warn(makeErr("no known meeting matching the 'id' field"));
    return false;
  }

  const meeting = new Meeting({
    ...oldMeeting,
    last_modified: mtgMsg.last_modified,
    location: mtgMsg.location,
    start: mtgMsg.start,
    end: mtgMsg.end,
    extra: {
      ...oldMeeting.extra,
      ...mtgMsg.extra,
    },
  });

  dispatch(updateEvent(lao.id, meeting.toState()));
  return true;
}

export function handleMeetingMessage(msg: ExtendedMessage) {
  if (msg.messageData.object !== ObjectType.MEETING) {
    console.warn('handleMeetingMessage was called to process an unsupported message', msg);
    return false;
  }

  switch (msg.messageData.action) {
    case ActionType.CREATE:
      return handleMeetingCreateMessage(msg);

    case ActionType.STATE:
      return handleMeetingStateMessage(msg);

    default:
      console.warn(
        'A Meeting message was received but' + ' its processing logic is not yet implemented:',
        msg,
      );
      return false;
  }
}
