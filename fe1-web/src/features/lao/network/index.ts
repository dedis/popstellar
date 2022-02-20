import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';

import {
  handleLaoCreateMessage,
  handleLaoStateMessage,
  handleLaoUpdatePropertiesMessage,
} from './LaoHandler';
import { CreateLao, StateLao, UpdateLao } from './messages';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configureNetwork(registry: MessageRegistry) {
  registry.add(ObjectType.LAO, ActionType.CREATE, handleLaoCreateMessage, CreateLao.fromJson);
  registry.add(ObjectType.LAO, ActionType.STATE, handleLaoStateMessage, StateLao.fromJson);
  registry.add(
    ObjectType.LAO,
    ActionType.UPDATE_PROPERTIES,
    handleLaoUpdatePropertiesMessage,
    UpdateLao.fromJson,
  );
}
