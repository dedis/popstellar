import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { NotificationReducerState, NOTIFICATION_REDUCER_PATH, NotificationState } from '../reducer';
import { NotificationFeature } from './Feature';

export const NOTIFICATION_FEATURE_IDENTIFIER = 'notification';

/**
 * The interface the notification feature exposes
 */
export interface NotificationConfigurationInterface extends FeatureInterface {
  components: {
    NotificationBadge: () => React.ReactNode;
  };

  laoScreens: NotificationFeature.LaoScreen[];

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
  useAssertCurrentLaoId: () => Hash;

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

    /**
     * Renders an icon for this notification type
     */
    Icon: React.ComponentType<{
      color: string;
      size: number;
    }>;
  }[];
}

/**
 * The type of the context that is provided to react components
 */
export type NotificationReactContext = Pick<
  NotificationCompositionConfiguration,
  'useAssertCurrentLaoId' | 'notificationTypes'
>;

export interface NotificationCompositionInterface extends FeatureInterface {
  context: NotificationReactContext;
}
