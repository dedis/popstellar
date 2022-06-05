import { SignatureType } from 'core/network/jsonrpc/messages';

import * as functions from './functions';
import {
  WalletCompositionConfiguration,
  WalletCompositionInterface,
  WalletInterface,
  WALLET_FEATURE_IDENTIFIER,
} from './interface';
import * as navigation from './navigation';
import { configureNetwork } from './network';
import { getCurrentPopTokenFromStore } from './objects';
import { digitalCashReducer, walletReducer } from './reducer';

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
  configureNetwork(configuration.messageRegistry);
  configuration.keyPairRegistry.add(
    SignatureType.POP_TOKEN,
    getCurrentPopTokenFromStore(configuration.getCurrentLao, configuration.getRollCallById),
  );
  return {
    identifier: WALLET_FEATURE_IDENTIFIER,
    navigation,
    reducers: {
      ...walletReducer,
      ...digitalCashReducer,
    },
    context: {
      useRollCallsByLaoId: configuration.useRollCallsByLaoId,
      useCurrentLaoId: configuration.useCurrentLaoId,
      getLaoOrganizer: configuration.getLaoOrganizer,
    },
  };
}
