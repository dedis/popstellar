import { WitnessNotificationType } from './components';
import { WitnessConfiguration, WitnessInterface, WITNESS_FEATURE_IDENTIFIER } from './interface';
import { configureNetwork } from './network';
import { witnessReducer } from './reducer';

/**
 * Configures the witness feature
 *
 * @param configuration - The witness configuration object
 * @returns The interface the witness feature exposes
 */
export function configure(configuration: WitnessConfiguration): WitnessInterface {
  configureNetwork(configuration);

  return {
    identifier: WITNESS_FEATURE_IDENTIFIER,

    notificationTypes: [WitnessNotificationType],

    context: {
      addNotification: configuration.addNotification,
      discardNotifications: configuration.discardNotifications,
      markNotificationAsRead: configuration.markNotificationAsRead,
    },
    reducers: {
      ...witnessReducer,
    },
  };
}
