import { ActionType, MessageRegistry, ObjectType } from 'core/network/jsonrpc/messages';

import { WitnessMessage } from './messages';
import { handleWitnessMessage } from './WitnessHandler';

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param registry - The MessageRegistry where we want to add the mappings
 */
export function configureNetwork(registry: MessageRegistry) {
  registry.add(
    ObjectType.MESSAGE,
    ActionType.WITNESS,
    handleWitnessMessage,
    WitnessMessage.fromJson,
  );
}
