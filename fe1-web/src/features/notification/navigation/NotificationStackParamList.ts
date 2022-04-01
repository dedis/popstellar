import STRINGS from 'resources/strings';

export type NotificationStackParamList = {
  [STRINGS.notification_navigation_tab_notifications]: undefined;
  [STRINGS.notification_navigation_tab_single_notification]: { notificationId: number };
};
