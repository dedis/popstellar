import {
  ActionType,
  AfterProcessingHandler,
  ObjectType,
  ProcessableMessage,
} from 'core/network/jsonrpc/messages';

import { WitnessConfiguration } from '../interface';
import { WitnessMessage } from './messages';
import { WitnessingType, getWitnessRegistryEntry } from './messages/WitnessRegistry';
import { handleWitnessMessage } from './WitnessHandler';
import { requestWitnessMessage } from './WitnessMessageApi';

/**
 * Is executed after a message has been successfully handled.
 * It handles the passive witnessing for messages and prepares
 * the application store for the act of manually witnessing
 * other messages
 */
const afterMessageProcessingHandler =
  (
    enabled: WitnessConfiguration['enabled'],
    /* isLaoWitness: WitnessConfiguration['isLaoWitness'] */
  ): AfterProcessingHandler =>
  (msg: ProcessableMessage) => {
    const entry = getWitnessRegistryEntry(msg.messageData);

    if (entry) {
      // we have a wintessing entry for this message type
      switch (entry.type) {
        case WitnessingType.PASSIVE:
          if (enabled) {
            requestWitnessMessage(msg.channel, msg.message_id);
          }
          break;

        case WitnessingType.ACTIVE:
          break;

        case WitnessingType.NO_WITNESSING:
        default:
          break;
      }
    }
  };

/**
 * Configures the network callbacks in a MessageRegistry.
 *
 * @param config - The witness feature configuration object
 */
export const configureNetwork = (config: WitnessConfiguration) => {
  config.messageRegistry.add(
    ObjectType.MESSAGE,
    ActionType.WITNESS,
    handleWitnessMessage(config.getCurrentLao),
    WitnessMessage.fromJson,
  );

  config.messageRegistry.addAfterProcessingHandler(
    afterMessageProcessingHandler(config.enabled /* config.isLaoWitness */),
  );
};
