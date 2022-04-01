import React from 'react';
import { AnyAction, Reducer } from 'redux';

import FeatureInterface from 'core/objects/FeatureInterface';

import { NotificationReducerState, NOTIFICATION_REDUCER_PATH } from '../reducer';
import { NotificationState } from '../reducer/NotificationReducer';

export const NOTIFICATION_FEATURE_IDENTIFIER = 'notification';

/**
 * The interface the notification feature exposes
 */
export interface NotificationInterface extends FeatureInterface {
  components: {
    NotificationBadge: () => React.ReactNode;
  };

  navigation: {
    NotificationNavigation: React.ComponentType<unknown>;
  };

  actionCreators: {
    addNotification: (notification: Omit<NotificationState, 'id' | 'hasBeenRead'>) => AnyAction;
    markNotificationAsRead: (notificationId: number) => AnyAction;
    discardNotification: (notificationId: number) => AnyAction;
  };

  reducers: {
    [NOTIFICATION_REDUCER_PATH]: Reducer<NotificationReducerState>;
  };
}
