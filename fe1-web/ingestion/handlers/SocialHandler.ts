import { ExtendedMessage } from 'model/network/method/message';
import { ActionType, AddChirp, ObjectType } from 'model/network/method/message/data';
import {
  addChirp,
  dispatch,
  getStore,
  makeCurrentLao,
} from 'store';
import { Chirp } from 'model/objects/Chirp';

/**
 * Handler for social media
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
 * Handles all social media messages by redirecting them to the correct function based on the
 * action.
 *
 * @param msg - The received extended message
 */
export function handleSocialMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.CHIRP) {
    console.warn('handleSocialMessage was called to process an unsupported message', msg);
    return false;
  }

  switch (msg.messageData.action) {
    case ActionType.ADD:
      return handleAddChirpMessage(msg);
    default:
      console.warn('A Social Media message was received but its processing logic is not '
        + 'yet implemented:', msg);
      return false;
  }
}
