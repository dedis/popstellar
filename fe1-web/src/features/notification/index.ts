import NotificationBadge from './components/NotificationBadge';
import {
  NotificationCompositionConfiguration,
  NotificationCompositionInterface,
  NotificationConfigurationInterface,
  NOTIFICATION_FEATURE_IDENTIFIER,
} from './interface/Configuration';
import { NotificationNavigationScreen } from './navigation';
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

  laoScreens: [NotificationNavigationScreen],

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
    useAssertCurrentLaoId: configuration.useAssertCurrentLaoId,
    notificationTypes: configuration.notificationTypes,
  },
});
