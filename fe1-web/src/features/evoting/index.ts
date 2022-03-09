import { configureNetwork } from './network';
import { EvotingConfiguration, EvotingInterface } from './objects';

export const EVOTING_FEATURE_IDENTIFIER = 'evoting';

/**
 * Configures the e-voting feature
 *
 * @param config - A evoting configuration object
 * @returns
 */
export const configure = (config: EvotingConfiguration): EvotingInterface => {
  const {
    getCurrentLao,
    getCurrentLaoId,
    getEventFromId,
    addEvent,
    updateEvent,
    onConfirmEventCreation,
  } = config;
  // configure message registry to correctly handle incoming messages
  configureNetwork(config);
  // return the interface that is exposed by the evoting feature
  return {
    /* this context will be used to pass the properties to react components */
    identifier: EVOTING_FEATURE_IDENTIFIER,
    context: {
      /* lao */
      getCurrentLao,
      getCurrentLaoId,
      /* event */
      getEventFromId,
      addEvent,
      updateEvent,
      onConfirmEventCreation,
    },
  };
};
