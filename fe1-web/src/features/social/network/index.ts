import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { SocialConfiguration } from '../interface';
import {
  handleAddChirpMessage,
  handleDeleteChirpMessage,
  handleNotifyAddChirpMessage,
  handleNotifyDeleteChirpMessage,
} from './ChirpHandler';
import { AddChirp, DeleteChirp, NotifyAddChirp, NotifyDeleteChirp } from './messages/chirp';
import { AddReaction, DeleteReaction } from './messages/reaction';
import { handleAddReactionMessage, handleDeleteReactionMessage } from './ReactionHandler';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param configuration - The configuration object for the social media feature.
 */
export function configureNetwork(configuration: SocialConfiguration) {
  // Chirp
  configuration.messageRegistry.add(
    ObjectType.CHIRP,
    ActionType.ADD,
    handleAddChirpMessage(configuration.getCurrentLaoId),
    AddChirp.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.CHIRP,
    ActionType.DELETE,
    handleDeleteChirpMessage(configuration.getCurrentLaoId),
    DeleteChirp.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.CHIRP,
    ActionType.NOTIFY_ADD,
    handleNotifyAddChirpMessage,
    NotifyAddChirp.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.CHIRP,
    ActionType.NOTIFY_DELETE,
    handleNotifyDeleteChirpMessage,
    NotifyDeleteChirp.fromJson,
  );

  // Reaction
  configuration.messageRegistry.add(
    ObjectType.REACTION,
    ActionType.ADD,
    handleAddReactionMessage(configuration.getCurrentLaoId),
    AddReaction.fromJson,
  );
  configuration.messageRegistry.add(
    ObjectType.REACTION,
    ActionType.DELETE,
    handleDeleteReactionMessage(configuration.getCurrentLaoId),
    DeleteReaction.fromJson,
  );
}
