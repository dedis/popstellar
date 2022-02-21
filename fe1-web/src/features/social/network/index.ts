import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';

import { AddChirp, DeleteChirp, NotifyAddChirp, NotifyDeleteChirp } from './messages/chirp';
import { AddReaction } from './messages/reaction';
import { handleAddReactionMessage } from './ReactionHandler';
import {
  handleAddChirpMessage,
  handleDeleteChirpMessage,
  handleNotifyAddChirpMessage,
  handleNotifyDeleteChirpMessage,
} from './ChirpHandler';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configureNetwork(registry: MessageRegistry) {
  // Chirp
  registry.add(ObjectType.CHIRP, ActionType.ADD, handleAddChirpMessage, AddChirp.fromJson);
  registry.add(ObjectType.CHIRP, ActionType.DELETE, handleDeleteChirpMessage, DeleteChirp.fromJson);
  registry.add(
    ObjectType.CHIRP,
    ActionType.NOTIFY_ADD,
    handleNotifyAddChirpMessage,
    NotifyAddChirp.fromJson,
  );
  registry.add(
    ObjectType.CHIRP,
    ActionType.NOTIFY_DELETE,
    handleNotifyDeleteChirpMessage,
    NotifyDeleteChirp.fromJson,
  );

  // Reaction
  registry.add(ObjectType.REACTION, ActionType.ADD, handleAddReactionMessage, AddReaction.fromJson);
}
