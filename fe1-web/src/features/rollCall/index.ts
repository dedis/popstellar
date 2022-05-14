import { RollCallEventType } from './components';
import * as functions from './functions';
import { RollCallHooks } from './hooks';
import { RollCallConfiguration, RollCallInterface, ROLLCALL_FEATURE_IDENTIFIER } from './interface';
import { configureNetwork } from './network';
import { rollCallReducer } from './reducer';
import * as screens from './screens';

/**
 * Configures the roll call feature
 *
 * @param configuration - The configuration object for the rollcall feature
 */
export function configure(configuration: RollCallConfiguration): RollCallInterface {
  configureNetwork(configuration);

  return {
    identifier: ROLLCALL_FEATURE_IDENTIFIER,
    eventTypes: [RollCallEventType],
    screens,
    functions,
    hooks: {
      useRollCallsByLaoId: RollCallHooks.useRollCallsByLaoId,
    },
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      makeEventByTypeSelector: configuration.makeEventByTypeSelector,
      generateToken: configuration.generateToken,
      hasSeed: configuration.hasSeed,
    },
    reducers: {
      ...rollCallReducer,
    },
  };
}
