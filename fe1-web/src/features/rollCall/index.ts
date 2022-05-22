import { RollCallEventTypeComponent } from './components';
import { RollCallConfiguration, RollCallInterface, ROLLCALL_FEATURE_IDENTIFIER } from './interface';
import { configureNetwork } from './network';
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
    eventTypeComponents: [RollCallEventTypeComponent],
    screens,
    context: {
      useCurrentLaoId: configuration.useCurrentLaoId,
      generateToken: configuration.generateToken,
      hasSeed: configuration.hasSeed,
      makeEventSelector: configuration.makeEventSelector,
    },
  };
}
