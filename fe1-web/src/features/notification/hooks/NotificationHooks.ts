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
   * A hook returning the current lao id
   * @returns The current lao id
   */
  export const useAssertCurrentLaoId = () => useNotificationContext().useAssertCurrentLaoId();

  /**
   * Gets the list of notification types with e.g. the component that should be used to
   * render them
   * @returns The list of different notification types
   */
  export const useNotificationTypes = () => useNotificationContext().notificationTypes;
}
