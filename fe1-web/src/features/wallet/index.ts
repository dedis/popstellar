import { SignatureType } from 'core/network/jsonrpc/messages';

import * as functions from './functions';
import {
  WalletCompositionConfiguration,
  WalletCompositionInterface,
  WalletInterface,
  WALLET_FEATURE_IDENTIFIER,
} from './interface';
import * as navigation from './navigation';
import { getCurrentPopTokenFromStore } from './objects';
import { walletReducer } from './reducer';
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
  configuration.keyPairRegistry.add(
    SignatureType.POP_TOKEN,
    getCurrentPopTokenFromStore(configuration.getCurrentLao, configuration.getRollCallById),
  );

  return {
    identifier: WALLET_FEATURE_IDENTIFIER,
    appScreens: [WalletCreateSeedScreen, WalletSetSeedScreen],
    navigation,
    reducers: {
      ...walletReducer,
    },
    context: {
      useRollCallsByLaoId: configuration.useRollCallsByLaoId,
      useCurrentLaoId: configuration.useCurrentLaoId,
    },
  };
}
