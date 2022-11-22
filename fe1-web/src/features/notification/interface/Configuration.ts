import React from 'react';
import { AnyAction, Reducer } from 'redux';

import { Hash } from 'core/objects';
import FeatureInterface from 'core/objects/FeatureInterface';

import { NotificationReducerState, NOTIFICATION_REDUCER_PATH } from '../reducer';
import { NotificationFeature } from './Feature';
import { Notification } from '../objects/Notification';
import { OmitMethods } from 'core/types';

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
    addNotification: (notification: Omit<OmitMethods<Notification>, 'id' | 'hasBeenRead'>) => AnyAction;
    markNotificationAsRead: (args: { laoId: Hash; notificationId: number }) => AnyAction;
    discardNotifications: (args: { laoId: Hash; notificationIds: number[] }) => AnyAction;
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
  useCurrentLaoId: () => Hash;

  notificationTypes: {
    /**
     * Checks if a given notification is of this type
     */
    isOfType: (notification: Notification) => boolean;

    /**
     * Callback function that is called when a notification is deleted
     */
    delete?: (notification: Notification) => void;

    /**
     * Renders the single notification view for this notification
     * type
     */
    Component: React.ComponentType<{
      notification: Notification;
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
  'useCurrentLaoId' | 'notificationTypes'
>;

export interface NotificationCompositionInterface extends FeatureInterface {
  context: NotificationReactContext;
}
