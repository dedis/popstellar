import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch, getStore } from 'core/redux';
import { selectCurrentLao } from 'features/lao/reducer';

import { Chirp } from '../objects';
import { addChirp, deleteChirp } from '../reducer';
import { AddChirp, DeleteChirp } from './messages/chirp';

/**
 * Handler for social media chirp
 */

/**
 * Handles an addChirp message by storing the chirp sent.
 *
 * @param msg - The extended message for adding a chirp
 */
export function handleAddChirpMessage(msg: ProcessableMessage): boolean {
  if (msg.messageData.object !== ObjectType.CHIRP || msg.messageData.action !== ActionType.ADD) {
    console.warn('handleAddChirp was called to process an unsupported message');
    return false;
  }

  const makeErr = (err: string) => `chirp/add was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = selectCurrentLao(storeState);
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
export function handleDeleteChirpMessage(msg: ProcessableMessage): boolean {
  if (msg.messageData.object !== ObjectType.CHIRP || msg.messageData.action !== ActionType.DELETE) {
    console.warn('handleDeleteChirp was called to process an unsupported message');
    return false;
  }

  const makeErr = (err: string) => `chirp/delete was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = selectCurrentLao(storeState);
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
 * TODO: Handles a NotifyAddChirp message.
 *
 * @param msg - The extended message for notifying a chirp addition
 */
export function handleNotifyAddChirpMessage(msg: ProcessableMessage): boolean {
  console.warn(`NotifyAddChirp message lacks of handling logic for now, message: ${msg}`);
  return true; // Pretend it has been handled
}

/**
 * TODO: Handles a NotifyDeleteChirp message.
 *
 * @param msg - The extended message for notifying a chirp deletion
 */
export function handleNotifyDeleteChirpMessage(msg: ProcessableMessage): boolean {
  console.warn(`NotifyDeleteChirp message lacks of handling logic for now, message: ${msg}`);
  return true; // Pretend it has been handled
}
