import { digitalCashWalletItemGenerator } from './components/DigitalCashWalletItems';
import {
  DigitalCashConfiguration,
  DigitalCashInterface,
  DIGITAL_CASH_FEATURE_IDENTIFIER,
} from './interface';
import { DigitalCashWalletScreen } from './screens/DigitalCashWallet';

/**
 * Configures the wallet feature
 */
export function configure(configuration: DigitalCashConfiguration): DigitalCashInterface {
  return {
    identifier: DIGITAL_CASH_FEATURE_IDENTIFIER,
    walletItemGenerators: [digitalCashWalletItemGenerator],
    walletScreens: [DigitalCashWalletScreen],
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      useIsLaoOrganizer: configuration.useIsLaoOrganizer,
    },
  };
}
