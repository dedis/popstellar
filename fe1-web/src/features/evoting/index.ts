import { ElectionEventType } from './components';
import { EvotingConfiguration, EvotingInterface, EVOTING_FEATURE_IDENTIFIER } from './interface';
import { configureNetwork } from './network';
import { electionReducer, electionKeyReducer } from './reducer';
import * as screens from './screens';

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
    useLaoOrganizerBackendPublicKey,
    getEventById,
    addEvent,
    updateEvent,
  } = config;
  // configure message registry to correctly handle incoming messages
  configureNetwork(config);
  // return the interface that is exposed by the evoting feature
  return {
    /* this context will be used to pass the properties to react components */
    identifier: EVOTING_FEATURE_IDENTIFIER,
    screens,
    eventTypes: [ElectionEventType],
    context: {
      /* lao */
      useCurrentLao,
      useCurrentLaoId,
      useLaoOrganizerBackendPublicKey,
      /* event */
      getEventById,
      addEvent,
      updateEvent,
    },
    reducers: {
      ...electionKeyReducer,
      ...electionReducer,
    },
  };
};
