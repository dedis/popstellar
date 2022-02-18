import { ExtendedMessage, MessageRegistry } from 'core/network/jsonrpc/messages';
import { dispatch, getStore } from 'core/redux';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages/MessageData';
import { makeCurrentLao } from 'features/lao/reducer';

import { Reaction } from '../objects';
import { AddReaction } from './messages/reaction';
import { addReaction } from '../reducer';

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
  if (msg.messageData.object !== ObjectType.REACTION || msg.messageData.action !== ActionType.ADD) {
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
    chirpId: reactionMessage.chirp_id,
    time: reactionMessage.timestamp,
  });

  dispatch(addReaction(lao.id, reaction.toState()));
  return true;
}

/**
 * Configures the ReactionHandler in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mapping
 */
export function configure(registry: MessageRegistry) {
  registry.addHandler(ObjectType.REACTION, ActionType.ADD, handleAddReactionMessage);
}
