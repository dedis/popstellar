import { configureNetwork } from './network';
import { EvotingConfiguration, EvotingInterface, EVOTING_FEATURE_IDENTIFIER } from './interface';

/**
 * Configures the e-voting feature
 *
 * @param config - A evoting configuration object
 * @returns
 */
export const configure = (config: EvotingConfiguration): EvotingInterface => {
  const {
    useCurrentLao,
    useCurrentLaoId,
    getEventById,
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
      useCurrentLao,
      useCurrentLaoId,
      /* event */
      getEventById,
      addEvent,
      updateEvent,
      onConfirmEventCreation,
    },
  };
};
