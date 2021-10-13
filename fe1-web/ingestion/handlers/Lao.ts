import { ExtendedMessage } from 'model/network/method/message';
import {
  ActionType, ObjectType, CreateLao, StateLao, UpdateLao,
} from 'model/network/method/message/data';
import { Lao } from 'model/objects';
import {
  dispatch, connectToLao, updateLao,
  getMessage, getStore,
  makeCurrentLao, makeLaoMessagesState,
} from 'store';
import { hasWitnessSignatureQuorum } from './Utils';

const getCurrentLao = makeCurrentLao();
const getMessageState = makeLaoMessagesState();

function handleLaoCreateMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.LAO || msg.messageData.action !== ActionType.CREATE) {
    console.warn('handleLaoCreateMessage was called to process an unsupported message', msg);
    return false;
  }

  const createLaoMsg = msg.messageData as CreateLao;
  const lao = new Lao({
    id: createLaoMsg.id,
    name: createLaoMsg.name,
    creation: createLaoMsg.creation,
    last_modified: createLaoMsg.creation,
    organizer: createLaoMsg.organizer,
    witnesses: createLaoMsg.witnesses,
  });

  dispatch(connectToLao(lao.toState()));
  return true;
}

function handleLaoStateMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.LAO || msg.messageData.action !== ActionType.STATE) {
    console.warn('handleLaoStateMessage was called to process an unsupported message', msg);
    return false;
  }

  const makeErr = (err: string) => `lao/state was not processed: ${err}`;

  const stateLaoData = msg.messageData as StateLao;
  if (!hasWitnessSignatureQuorum(stateLaoData.modification_signatures)) {
    console.warn(makeErr('witness quorum was not reached'));
    return false;
  }

  const storeState = getStore().getState();
  const oldLao = getCurrentLao(storeState);
  if (!oldLao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const msgState = getMessageState(storeState);
  if (!msgState) {
    console.warn(makeErr("no known 'lao/update_properties' messages in LAO"));
    return false;
  }

  const updateMessage = getMessage(msgState, stateLaoData.modification_id);
  if (!updateMessage) {
    console.warn(makeErr("'modification_id' references unknown message"));
    return false;
  }
  const updateLaoData = updateMessage.messageData as UpdateLao;

  const lao = new Lao({
    ...oldLao,
    name: updateLaoData.name,
    witnesses: updateLaoData.witnesses,
  });

  dispatch(updateLao(lao.toState()));
  return true;
}

export function handleLaoMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.LAO) {
    console.warn('handleLaoMessage was called to process an unsupported message', msg);
    return false;
  }

  switch (msg.messageData.action) {
    case ActionType.CREATE:
      return handleLaoCreateMessage(msg);

    case ActionType.STATE:
      return handleLaoStateMessage(msg);

    case ActionType.UPDATE_PROPERTIES:
      console.debug('lao/update_properties message was archived: no action needs to be taken');
      return true;

    default:
      console.warn('A LAO message was received but'
        + ' its processing logic is not yet implemented:', msg);
      return false;
  }
}
