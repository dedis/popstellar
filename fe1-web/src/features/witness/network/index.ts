import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';

import { WitnessConfiguration } from '../interface';
import { WitnessMessage } from './messages';
import { handleWitnessMessage } from './WitnessHandler';

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
};
