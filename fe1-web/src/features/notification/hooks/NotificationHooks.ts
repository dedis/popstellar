import { useContext } from 'react';

import FeatureContext from 'core/contexts/FeatureContext';

import {
  NotificationReactContext,
  NOTIFICATION_FEATURE_IDENTIFIER,
} from '../interface/Configuration';

export namespace NotificationHooks {
  /* Hooks passed by dependencies */

  export const useNotificationContext = (): NotificationReactContext => {
    const featureContext = useContext(FeatureContext);
    // assert that the notification context exists
    if (!(NOTIFICATION_FEATURE_IDENTIFIER in featureContext)) {
      throw new Error('Notification context could not be found!');
    }
    return featureContext[NOTIFICATION_FEATURE_IDENTIFIER] as NotificationReactContext;
  };

  /**
   * Gets the list of components that can be used to render components
   * @returns The list of components used to render notifications
   */
  export const useNotificationTypeComponents = () =>
    useNotificationContext().notificationTypeComponents;
}
