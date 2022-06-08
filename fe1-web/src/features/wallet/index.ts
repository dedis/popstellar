import { SignatureType } from 'core/network/jsonrpc/messages';

import * as functions from './functions';
import {
  WalletCompositionConfiguration,
  WalletCompositionInterface,
  WalletInterface,
  WALLET_FEATURE_IDENTIFIER,
} from './interface';
import { WalletNavigationScreen } from './navigation/WalletNavigation';
import { configureNetwork } from '../digital-cash/network';
import { getCurrentPopTokenFromStore } from './objects';
import { digitalCashReducer, walletReducer } from './reducer';
import { WalletCreateSeedScreen } from './screens/WalletCreateSeed';
import { WalletSetSeedScreen } from './screens/WalletSetSeed';

/**
 * Configures the wallet feature
 */
export function configure(): WalletInterface {
  return {
    identifier: WALLET_FEATURE_IDENTIFIER,
    functions,
  };
}

export function compose(configuration: WalletCompositionConfiguration): WalletCompositionInterface {
  configureNetwork(configuration.messageRegistry, configuration.getLaoOrganizer);
  configuration.keyPairRegistry.add(
    SignatureType.POP_TOKEN,
    getCurrentPopTokenFromStore(configuration.getCurrentLao, configuration.getRollCallById),
  );
  return {
    identifier: WALLET_FEATURE_IDENTIFIER,
    appScreens: [WalletCreateSeedScreen, WalletSetSeedScreen],
    homeScreens: [WalletNavigationScreen],
    laoScreens: [WalletNavigationScreen],
    reducers: {
      ...walletReducer,
      ...digitalCashReducer,
    },
    context: {
      useRollCallsByLaoId: configuration.useRollCallsByLaoId,
      useCurrentLaoId: configuration.useCurrentLaoId,
      getLaoOrganizer: configuration.getLaoOrganizer,
      useLaoIds: configuration.useLaoIds,
      useNamesByLaoId: configuration.useNamesByLaoId,
      walletItemGenerators: configuration.walletItemGenerators,
      walletNavigationScreens: configuration.walletNavigationScreens,
    },
  };
}
