import { ExtendedMessage } from 'model/network/method/message';
import {
  addReaction, dispatch, getStore, makeCurrentLao,
} from 'store';
import { ActionType, AddReaction, ObjectType } from 'model/network/method/message/data';
import { Reaction } from 'model/objects/Reaction';

/**
 * Handler for social media chirp's reactions
 */
const getCurrentLao = makeCurrentLao();

/**
 * Handles an addReaction message by storing the reaction sent.
 *
 * @param msg - The extended message for adding a reaction
 */
function handleAddReactionMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.REACTION
    || msg.messageData.action !== ActionType.ADD) {
    console.warn('handleAddReaction was called to process an unsupported message');
    return false;
  }

  const makeErr = (err: string) => `reaction/add was not processed: ${err}`;

  const storeState = getStore().getState();
  const lao = getCurrentLao(storeState);
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
    chirp_id: reactionMessage.chirp_id,
    time: reactionMessage.timestamp,
  });

  dispatch(addReaction(lao.id, reaction.toState()));
  return true;
}

/**
 * Handles all social media reaction messages by redirecting them to the correct function based on
 * the action.
 *
 * @param msg - The received extended message
 */
export function handleReactionMessage(msg: ExtendedMessage): boolean {
  if (msg.messageData.object !== ObjectType.REACTION) {
    console.warn('handleReactionMessage was called to process an unsupported message', msg);
    return false;
  }

  switch (msg.messageData.action) {
    case ActionType.ADD:
      return handleAddReactionMessage(msg);
    default:
      console.warn('A Social Media reaction message was received but its processing logic is not '
        + 'yet implemented:', msg);
      return false;
  }
}
