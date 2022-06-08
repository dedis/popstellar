import { digitalCashWalletItemGenerator } from './components/DigitalCashWalletItems';
import {
  DigitalCashInterface,
  DIGITAL_CASH_FEATURE_IDENTIFIER,
  DigitalCashCompositionConfiguration,
  DigitalCashCompositionInterface,
} from './interface';
import { configureNetwork } from './network';
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
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      useIsLaoOrganizer: configuration.useIsLaoOrganizer,
      useRollCallTokensByLaoId: configuration.useRollCallTokensByLaoId,
    },
  };
}
