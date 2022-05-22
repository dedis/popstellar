import { SignatureType } from 'core/network/jsonrpc/messages';

import * as functions from './functions';
import { WalletConfiguration, WalletInterface, WALLET_FEATURE_IDENTIFIER } from './interface';
import * as navigation from './navigation';
import { getCurrentPopTokenFromStore } from './objects';
import { walletReducer } from './reducer';

/**
 * Configures the wallet feature
 */
export function configure(configuration: WalletConfiguration): WalletInterface {
  configuration.keyPairRegistry.add(
    SignatureType.POP_TOKEN,
    getCurrentPopTokenFromStore(configuration.getCurrentLao, configuration.getEventById),
  );

  return {
    identifier: WALLET_FEATURE_IDENTIFIER,
    navigation,
    functions,
    reducers: {
      ...walletReducer,
    },
    context: {
      makeEventByTypeSelector: configuration.makeEventByTypeSelector,
      useCurrentLaoId: configuration.useCurrentLaoId,
    },
  };
}
