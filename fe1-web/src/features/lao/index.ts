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
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { laoReducer, addLaoServerAddress } from './reducer';
import ServerReducer from './reducer/ServerReducer';

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
    },
    hooks: {
      useLaoList: hooks.LaoHooks.useLaoList,
      useIsLaoOrganizer: hooks.LaoHooks.useIsLaoOrganizer,
      useIsLaoWitness: hooks.LaoHooks.useIsLaoWitness,
      useLaoMap: hooks.LaoHooks.useLaoMap,
      useCurrentLao: hooks.LaoHooks.useCurrentLao,
      useCurrentLaoId: hooks.LaoHooks.useCurrentLaoId,
      useLaoOrganizerBackendPublicKey: hooks.LaoHooks.useLaoOrganizerBackendPublicKey,
    },
    functions,
    reducers: {
      ...laoReducer,
      ...ServerReducer,
    },
  };
};

export const compose = (config: LaoCompositionConfiguration): LaoCompositionInterface => {
  return {
    identifier: LAO_FEATURE_IDENTIFIER,
    navigation,
    context: {
      EventList: config.EventList,
      encodeLaoConnectionForQRCode: config.encodeLaoConnectionForQRCode,
      laoNavigationScreens: config.laoNavigationScreens,
      organizerNavigationScreens: config.organizerNavigationScreens,
    },
  };
};
