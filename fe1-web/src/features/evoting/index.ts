import { configureNetwork } from './network';
import { EvotingConfiguration } from './objects';

/**
 * Configures the e-voting feature
 *
 * @param config - A evoting configuration object
 */
export function configure(config: EvotingConfiguration) {
  configureNetwork(
    config.getCurrentLao,
    config.getCurrentLaoId,
    config.getEventFromId,
    config.addEvent,
    config.updateEvent,
    config.messageRegistry,
  );
}
