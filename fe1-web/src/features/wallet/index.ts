import { SignatureType } from 'core/network/jsonrpc/messages';

import { WalletConfiguration, WalletInterface, WALLET_FEATURE_IDENTIFIER } from './interface';
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { getCurrentPopTokenFromStore } from './objects';
import { walletReducer } from './reducer';

/**
 * Configures the wallet feature
 */
export function configure(configuration: WalletConfiguration): WalletInterface {
  configureNetwork(configuration.messageRegistry);
  configuration.keyPairRegistry.add(
    SignatureType.POP_TOKEN,
    getCurrentPopTokenFromStore(configuration.getCurrentLao, configuration.getEventById),
  );
  return {
    identifier: WALLET_FEATURE_IDENTIFIER,
    navigation,
    reducers: {
      ...walletReducer,
    },
    context: {
      makeEventByTypeSelector: configuration.makeEventByTypeSelector,
      useCurrentLaoId: configuration.useCurrentLaoId,
    },
  };
}
