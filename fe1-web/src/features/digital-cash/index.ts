import {
  DigitalCashInterface,
  DIGITAL_CASH_FEATURE_IDENTIFIER,
  DigitalCashCompositionConfiguration,
  DigitalCashCompositionInterface,
} from './interface';
import { DigitalCashLaoScreen } from './navigation/DigitalCashNavigation';
import { configureNetwork } from './network';
import { digitalCashReducer } from './reducer';

/**
 * Configures the wallet feature
 */
export function configure(): DigitalCashInterface {
  return {
    identifier: DIGITAL_CASH_FEATURE_IDENTIFIER,
    laoScreens: [DigitalCashLaoScreen],
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
      useCurrentLaoId: configuration.useCurrentLaoId,
      useConnectedToLao: configuration.useConnectedToLao,
      useIsLaoOrganizer: configuration.useIsLaoOrganizer,
      useRollCallTokensByLaoId: configuration.useRollCallTokensByLaoId,
      useRollCallTokenByRollCallId: configuration.useRollCallTokenByRollCallId,
      useRollCallsByLaoId: configuration.useRollCallsByLaoId,
    },
  };
}
