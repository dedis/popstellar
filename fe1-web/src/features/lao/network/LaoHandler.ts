import { ExtendedMessage, MessageRegistry } from 'core/network/jsonrpc/messages';
import { ActionType, ObjectType } from 'core/network/validation/Validator';
import { hasWitnessSignatureQuorum } from 'core/network/validation/Checker';
import { getMessage, makeLaoMessagesState } from 'core/reducers';
import { dispatch, getStore } from 'core/redux';

import { Lao } from '../objects';
import { connectToLao, makeCurrentLao, updateLao } from '../reducer';
import { CreateLao, StateLao, UpdateLao } from './messages';

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

  const storeState = getStore().getState();
  const oldLao = getCurrentLao(storeState);
  if (!oldLao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const stateLaoData = msg.messageData as StateLao;
  if (!hasWitnessSignatureQuorum(stateLaoData.modification_signatures, oldLao)) {
    console.warn(makeErr('witness quorum was not reached'));
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

function handleLaoUpdatePropertiesMessage(msg: ExtendedMessage): boolean {
  console.debug(`lao/update_properties message was archived: no action needs to be taken ${msg}`);
  return true;
}

/**
 * Configures the LaoHandler in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  registry.addHandler(ObjectType.LAO, ActionType.CREATE, handleLaoCreateMessage);
  registry.addHandler(ObjectType.LAO, ActionType.STATE, handleLaoStateMessage);
  registry.addHandler(
    ObjectType.LAO,
    ActionType.UPDATE_PROPERTIES,
    handleLaoUpdatePropertiesMessage,
  );
}
