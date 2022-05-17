import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { NotificationReducerState, NOTIFICATION_REDUCER_PATH } from '../reducer';
import { NotificationState } from '../reducer/NotificationReducer';

export const NOTIFICATION_FEATURE_IDENTIFIER = 'notification';

/**
 * The interface the notification feature exposes
 */
export interface NotificationConfigurationInterface extends FeatureInterface {
  components: {
    NotificationBadge: () => React.ReactNode;
  };

  navigation: {
    NotificationNavigation: React.ComponentType<unknown>;
  };

  actionCreators: {
    addNotification: (notification: Omit<NotificationState, 'id' | 'hasBeenRead'>) => AnyAction;
    markNotificationAsRead: (args: { laoId: string; notificationId: number }) => AnyAction;
    discardNotifications: (args: { laoId: string; notificationIds: number[] }) => AnyAction;
  };

  reducers: {
    [NOTIFICATION_REDUCER_PATH]: Reducer<NotificationReducerState>;
  };
}

export interface NotificationCompositionConfiguration {
  /**
   * A hook returning the current lao id
   * @returns The current lao id
   */
  useCurrentLaoId: () => Hash | undefined;

  notificationTypes: {
    /**
     * Checks if a given notification is of this type
     */
    isOfType: (notification: NotificationState) => boolean;

    /**
     * Callback function that is called when a notification is deleted
     */
    delete?: (notification: NotificationState) => void;

    /**
     * Renders the single notification view for this notification
     * type
     */
    Component: React.ComponentType<{
      notification: NotificationState;
      navigateToNotificationScreen: () => void;
    }>;
  }[];
}

/**
 * The type of the context that is provided to react components
 */
export type NotificationReactContext = Pick<
  NotificationCompositionConfiguration,
  'useCurrentLaoId' | 'notificationTypes'
>;

export interface NotificationCompositionInterface extends FeatureInterface {
  context: NotificationReactContext;
}
