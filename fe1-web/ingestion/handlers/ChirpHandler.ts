import { ExtendedMessage } from 'model/network/method/message';
import {
  ActionType, AddChirp, ObjectType, DeleteChirp,
} from 'model/network/method/message/data';
import {
  addChirp,
  dispatch,
  getStore,
  makeCurrentLao,
  deleteChirp,
} from 'store';
import { Chirp } from 'model/objects/Chirp';

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
 * Handles all social media chirp messages by redirecting them to the correct function based on the
 * action.
 *
 * @param msg - The received extended message
 */
export function handleChirpMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.CHIRP) {
    console.warn('handleChirpMessage was called to process an unsupported message', msg);
    return false;
  }

  switch (msg.messageData.action) {
    case ActionType.ADD:
      return handleAddChirpMessage(msg);
    case ActionType.DELETE:
      return handleDeleteChirpMessage(msg);
    default:
      console.warn('A Social Media chirp message was received but its processing logic is not '
        + 'yet implemented:', msg);
      return false;
  }
}
