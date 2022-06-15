import { PublicComponents } from './components';
import * as functions from './functions';
import * as hooks from './hooks';
import {
  LaoCompositionConfiguration,
  LaoCompositionInterface,
  LaoConfiguration,
  LaoConfigurationInterface,
  LAO_FEATURE_IDENTIFIER,
} from './interface';
import { LaoNavigationAppScreen } from './navigation';
import { configureNetwork } from './network';
import {
  laoReducer,
  serverReducer,
  addLaoServerAddress,
  greetLaoReducer,
  setLaoLastRollCall,
} from './reducer';

/**
 * Configures the LAO feature
 *
 * @param config - The configuration object
 */

export const configure = (config: LaoConfiguration): LaoConfigurationInterface => {
  configureNetwork(config.registry);

  return {
    identifier: LAO_FEATURE_IDENTIFIER,
    components: PublicComponents,
    actionCreators: {
      addLaoServerAddress,
      setLaoLastRollCall,
    },
    hooks: {
      useLaoList: hooks.LaoHooks.useLaoList,
      useIsLaoOrganizer: hooks.LaoHooks.useIsLaoOrganizer,
      useIsLaoWitness: hooks.LaoHooks.useIsLaoWitness,
      useLaoMap: hooks.LaoHooks.useLaoMap,
      useCurrentLao: hooks.LaoHooks.useCurrentLao,
      useCurrentLaoId: hooks.LaoHooks.useCurrentLaoId,
      useLaoOrganizerBackendPublicKey: hooks.LaoHooks.useLaoOrganizerBackendPublicKey,
      useDisconnectFromLao: hooks.LaoHooks.useDisconnectFromLao,
    },
    functions,
    reducers: {
      ...laoReducer,
      ...serverReducer,
      ...greetLaoReducer,
    },
  };
};

export const compose = (config: LaoCompositionConfiguration): LaoCompositionInterface => {
  return {
    identifier: LAO_FEATURE_IDENTIFIER,
    appScreens: [LaoNavigationAppScreen],
    context: {
      EventList: config.EventList,
      CreateEventButton: config.CreateEventButton,
      encodeLaoConnectionForQRCode: config.encodeLaoConnectionForQRCode,
      laoNavigationScreens: config.laoNavigationScreens,
      eventsNavigationScreens: config.eventsNavigationScreens,
    },
  };
};
