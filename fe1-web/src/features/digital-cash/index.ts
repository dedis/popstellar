import { digitalCashWalletItemGenerator } from './components/DigitalCashWalletItems';
import {
  DigitalCashInterface,
  DIGITAL_CASH_FEATURE_IDENTIFIER,
  DigitalCashCompositionConfiguration,
  DigitalCashCompositionInterface,
} from './interface';
import { configureNetwork } from './network';
import { digitalCashReducer } from './reducer';
import { DigitalCashWalletScreen } from './screens/DigitalCashWallet';
import { PoPTokenScannerScreen } from './screens/PoPTokenScanner';
import { SendReceiveScreen } from './screens/SendReceive';

/**
 * Configures the wallet feature
 */
export function configure(): DigitalCashInterface {
  return {
    identifier: DIGITAL_CASH_FEATURE_IDENTIFIER,
    walletItemGenerators: [digitalCashWalletItemGenerator],
    walletScreens: [DigitalCashWalletScreen, SendReceiveScreen, PoPTokenScannerScreen],
  };
}

export function compose(
  configuration: DigitalCashCompositionConfiguration,
): DigitalCashCompositionInterface {
  configureNetwork(configuration.messageRegistry, configuration.getLaoOrganizer);
  return {
    identifier: DIGITAL_CASH_FEATURE_IDENTIFIER,
    reducers: {
      ...digitalCashReducer,
    },
    context: {
      useRollCallById: configuration.useRollCallById,
      useAssertCurrentLaoId: configuration.useAssertCurrentLaoId,
      useConnectedToLao: configuration.useConnectedToLao,
      useIsLaoOrganizer: configuration.useIsLaoOrganizer,
      useRollCallTokensByLaoId: configuration.useRollCallTokensByLaoId,
      useRollCallTokenByRollCallId: configuration.useRollCallTokenByRollCallId,
      useRollCallsByLaoId: configuration.useRollCallsByLaoId,
    },
  };
}
