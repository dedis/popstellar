import { ExtendedMessage } from 'model/network/method/message';
import {
  ActionType,
  AddChirp,
  DeleteChirp,
  MessageRegistry,
  ObjectType,
} from 'model/network/method/message/data';
import {
  addChirp, deleteChirp, dispatch, getStore, makeCurrentLao,
} from 'store';
import { Chirp } from 'model/objects';

/**
 * Handler for social media chirp
 */
const getCurrentLao = makeCurrentLao();

/**
 * Handles an addChirp message by storing the chirp sent.
 *
 * @param msg - The extended message for adding a chirp
 */
function handleAddChirpMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.CHIRP
    || msg.messageData.action !== ActionType.ADD) {
    console.warn('handleAddChirp was called to process an unsupported message');
    return false;
  }

  const makeErr = (err: string) => `chirp/add was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const messageId = msg.message_id;
  const { sender } = msg;
  const chirpMessage = msg.messageData as AddChirp;

  const chirp = new Chirp({
    id: messageId,
    sender: sender,
    text: chirpMessage.text,
    time: chirpMessage.timestamp,
    parentId: chirpMessage.parent_id,
  });

  dispatch(addChirp(lao.id, chirp.toState()));
  return true;
}

/**
 * Handles an deleteChirp message
 *
 * @param msg - The extended message for deleting a chirp
 */
function handleDeleteChirpMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.CHIRP
    || msg.messageData.action !== ActionType.DELETE) {
    console.warn('handleDeleteChirp was called to process an unsupported message');
    return false;
  }

  const makeErr = (err: string) => `chirp/delete was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no LAO is currently active'));
    return false;
  }

  const { sender } = msg;
  const chirpMessage = msg.messageData as DeleteChirp;

  const chirp = new Chirp({
    id: chirpMessage.chirp_id,
    sender: sender,
    time: chirpMessage.timestamp,
    text: '',
  });

  dispatch(deleteChirp(lao.id, chirp.toState()));
  return true;
}

/**
 * Configures the ChirpHandler in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configure(registry: MessageRegistry) {
  registry.addHandler(ObjectType.CHIRP, ActionType.ADD, handleAddChirpMessage);
  registry.addHandler(ObjectType.CHIRP, ActionType.DELETE, handleDeleteChirpMessage);
}
