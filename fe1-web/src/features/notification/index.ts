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
  discardNotifications,
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
    discardNotifications,
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
    useCurrentLaoId: configuration.useCurrentLaoId,
    notificationTypes: configuration.notificationTypes,
  },
});
