import STRINGS from 'resources/strings';
import { configureNetwork } from './network';
import { PublicComponents } from './components';
import * as hooks from './hooks';
import * as functions from './functions';
import * as navigation from './navigation';
import { laoReducer, setLaoServerAddress } from './reducer';
import {
  LaoCompositionConfiguration,
  LaoCompositionInterface,
  LaoConfiguration,
  LaoConfigurationInterface,
  LAO_FEATURE_IDENTIFIER,
} from './interface';
import { Identity } from './screens';

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
      setLaoServerAddress,
    },
    hooks: {
      useLaoList: hooks.LaoHooks.useLaoList,
      useIsLaoOrganizer: hooks.LaoHooks.useIsLaoOrganizer,
      useLaoMap: hooks.LaoHooks.useLaoMap,
      useCurrentLao: hooks.LaoHooks.useCurrentLao,
      useCurrentLaoId: hooks.LaoHooks.useCurrentLaoId,
    },
    functions,
    reducers: {
      ...laoReducer,
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
      laoNavigationScreens: [
        ...config.laoNavigationScreens,
        { name: STRINGS.organization_navigation_tab_identity, Component: Identity, order: 2 },
      ],
      organizerNavigationScreens: config.organizerNavigationScreens,
    },
  };
};
