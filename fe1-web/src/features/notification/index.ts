import NotificationBadge from './components/NotificationBadge';
import { NotificationInterface, NOTIFICATION_FEATURE_IDENTIFIER } from './interface/Configuration';
import NotificationNavigation from './navigation/NotificationNavigation';
import {
  addNotification,
  discardNotification,
  markNotificationAsRead,
  notificationReducer,
} from './reducer';

export const configure = (): NotificationInterface => ({
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
