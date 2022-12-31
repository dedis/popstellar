import { ActionType, ObjectType, ProcessableMessage } from 'core/network/jsonrpc/messages';
import { dispatch } from 'core/redux';

import { SocialConfiguration } from '../interface';
import { Reaction } from '../objects';
import { addReaction } from '../reducer';
import { AddReaction } from './messages/reaction';

/**
 * Handler for social media chirp's reactions
 */

/**
 * Handles an addReaction message by storing the reaction sent.
 */
export const handleAddReactionMessage =
  (getCurrentLaoId: SocialConfiguration['getCurrentLaoId']) => (msg: ProcessableMessage) => {
    if (
      msg.messageData.object !== ObjectType.REACTION ||
      msg.messageData.action !== ActionType.ADD
    ) {
      console.warn('handleAddReaction was called to process an unsupported message');
      return false;
    }

    const makeErr = (err: string) => `reaction/add was not processed: ${err}`;

    const laoId = getCurrentLaoId();
    if (!laoId) {
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

    dispatch(addReaction(laoId, reaction));
    return true;
  };
