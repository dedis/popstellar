import { ElectionEventType } from './components';
import { EvotingConfiguration, EvotingInterface, EVOTING_FEATURE_IDENTIFIER } from './interface';
import { configureNetwork } from './network';
import { electionReducer, electionKeyReducer } from './reducer';
import { CreateElectionScreen, ViewSingleElectionScreen } from './screens';

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
    useConnectedToLao,
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
    laoEventScreens: [CreateElectionScreen, ViewSingleElectionScreen],
    eventTypes: [ElectionEventType],
    context: {
      /* lao */
      useCurrentLao,
      useCurrentLaoId,
      useConnectedToLao,
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
