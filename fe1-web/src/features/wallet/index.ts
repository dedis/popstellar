import { SignatureType } from 'core/network/jsonrpc/messages';
import { Hash } from 'core/objects';

import * as functions from './functions';
import {
  WalletCompositionConfiguration,
  WalletCompositionInterface,
  WalletInterface,
  WALLET_FEATURE_IDENTIFIER,
} from './interface';
import { WalletNavigationScreen } from './navigation/WalletNavigation';
import { getCurrentPopTokenFromStore, recoverWalletRollCallTokens } from './objects';
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
    functions: {
      useRollCallTokensByLaoId: (laoId: string) =>
        recoverWalletRollCallTokens(
          Object.values(configuration.useRollCallsByLaoId(laoId)),
          new Hash(laoId),
        ),
    },
    context: {
      useRollCallsByLaoId: configuration.useRollCallsByLaoId,
      useCurrentLaoId: configuration.useCurrentLaoId,
      useLaoIds: configuration.useLaoIds,
      useNamesByLaoId: configuration.useNamesByLaoId,
      walletItemGenerators: configuration.walletItemGenerators,
      walletNavigationScreens: configuration.walletNavigationScreens,
    },
  };
}
