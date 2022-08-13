import { RollCallEventType } from './components';
import * as functions from './functions';
import { RollCallHooks } from './hooks';
import { ROLLCALL_FEATURE_IDENTIFIER, RollCallConfiguration, RollCallInterface } from './interface';
import { configureNetwork } from './network';
import { rollCallReducer } from './reducer';
import { CreateRollCallScreen, RollCallOpenedScreen, ViewSingleRollCallScreen } from './screens';

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
    laoEventScreens: [CreateRollCallScreen, RollCallOpenedScreen, ViewSingleRollCallScreen],
    functions,
    hooks: {
      useRollCallById: RollCallHooks.useRollCallById,
      useRollCallsByLaoId: RollCallHooks.useRollCallsByLaoId,
      useRollCallTokensByLaoId: RollCallHooks.useRollCallTokensByLaoId,
      useRollCallTokenByRollCallId: RollCallHooks.useRollCallTokenByRollCallId,
      useRollCallAttendeesById: RollCallHooks.useRollCallAttendeesById,
    },
    context: {
      useAssertCurrentLaoId: configuration.useAssertCurrentLaoId,
      makeEventByTypeSelector: configuration.makeEventByTypeSelector,
      generateToken: configuration.generateToken,
      hasSeed: configuration.hasSeed,
    },
    reducers: {
      ...rollCallReducer,
    },
  };
}
