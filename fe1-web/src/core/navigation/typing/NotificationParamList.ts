import STRINGS from 'resources/strings';

export type NotificationParamList = {
  [STRINGS.navigation_notification_notifications]: undefined;
  [STRINGS.navigation_notification_single_notification]: { notificationId: number };
};
