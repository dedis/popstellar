import { ExtendedMessage } from 'model/network/method/message';
import { ActionType, AddChirp, ObjectType } from 'model/network/method/message/data';
import { getStore, KeyPairStore, makeCurrentLao } from 'store';
import { Chirp } from 'model/objects/Chirp';

/**
 * Handler for social media
 */

const getCurrentLao = makeCurrentLao();

/**
 * Handles an addChirp message by storing it.
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

  const chirpMessage = msg.messageData as AddChirp;

  const chirp = new Chirp({
    sender: KeyPairStore.get().publicKey.valueOf(),
    text: chirpMessage.text,
    time: chirpMessage.timestamp,
    likes: 0,
    dislikes: 0,
    parentId: chirpMessage.parent_id,
  });
  return true;
}
