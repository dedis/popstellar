import { configureNetwork } from './network';
import { EvotingConfiguration } from './objects';

let evotingConfig: EvotingConfiguration;

/**
 * Configures the e-voting feature
 *
 * @param config - A evoting configuration object
 */
export const configure = (config: EvotingConfiguration) => {
  configureNetwork(
    config.getCurrentLao,
    config.getEventFromId,
    config.addEvent,
    config.updateEvent,
    config.messageRegistry,
  );

  // store config so that react components can retrieve the values as well
  evotingConfig = config;
};

/**
 * Retrieves the evoting configuration
 * @returns The current evoting configuration
 */
export const getEvotingConfig = () => {
  return evotingConfig;
};
