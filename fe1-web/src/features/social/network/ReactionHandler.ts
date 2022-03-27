import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch, getStore } from 'core/redux';
import { selectCurrentLao } from 'features/lao/reducer';

import { Reaction } from '../objects';
import { addReaction } from '../reducer';
import { AddReaction } from './messages/reaction';

/**
 * Handler for social media chirp's reactions
 */

/**
 * Handles an addReaction message by storing the reaction sent.
 *
 * @param msg - The extended message for adding a reaction
 */
export function handleAddReactionMessage(msg: ProcessableMessage): boolean {
  if (msg.messageData.object !== ObjectType.REACTION || msg.messageData.action !== ActionType.ADD) {
    console.warn('handleAddReaction was called to process an unsupported message');
    return false;
  }

  const makeErr = (err: string) => `reaction/add was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = selectCurrentLao(storeState);
  if (!lao) {
    console.warn(makeErr('no Lao is currently active'));
    return false;
  }

  const messageId = msg.message_id;
  const { sender } = msg;
  const reactionMessage = msg.messageData as AddReaction;

  const reaction = new Reaction({
    id: messageId,
    sender: sender,
    codepoint: reactionMessage.reaction_codepoint,
    chirpId: reactionMessage.chirp_id,
    time: reactionMessage.timestamp,
  });

  dispatch(addReaction(lao.id, reaction.toState()));
  return true;
}
