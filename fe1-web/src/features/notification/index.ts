import NotificationBadge from './components/NotificationBadge';
import {
  NotificationCompositionConfiguration,
  NotificationCompositionInterface,
  NotificationConfigurationInterface,
  NOTIFICATION_FEATURE_IDENTIFIER,
} from './interface/Configuration';
import NotificationNavigation from './navigation/NotificationNavigation';
import {
  addNotification,
  discardNotification,
  markNotificationAsRead,
  notificationReducer,
} from './reducer';

export const configure = (): NotificationConfigurationInterface => ({
  identifier: NOTIFICATION_FEATURE_IDENTIFIER,

  components: {
    NotificationBadge,
  },

  navigation: {
    NotificationNavigation,
  },

  actionCreators: {
    addNotification,
    discardNotification,
    markNotificationAsRead,
  },

  reducers: {
    ...notificationReducer,
  },
});

export const compose = (
  configuration: NotificationCompositionConfiguration,
): NotificationCompositionInterface => ({
  identifier: NOTIFICATION_FEATURE_IDENTIFIER,
  context: {
    notificationTypeComponents: configuration.notificationTypeComponents,
  },
});
