import React, { useMemo } from 'react';
import { View } from 'react-native';
import { useSelector } from 'react-redux';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { dispatch } from 'core/redux';
import { Color, Icon, List } from 'core/styles';
import STRINGS from 'resources/strings';

import NotificationList from '../components/NotificationList';
import { NotificationHooks } from '../hooks';
import {
  discardNotifications,
  makeReadNotificationsSelector,
  makeUnreadNotificationsSelector,
} from '../reducer';

/**
 * The notification screen component displaying the list of read and unread notifications
 */
const NotificationScreen = () => {
  const laoId = NotificationHooks.useAssertCurrentLaoId();

  const selectUnreadNotifications = useMemo(
    () => makeUnreadNotificationsSelector(laoId.valueOf()),
    [laoId],
  );
  const selectReadNotifications = useMemo(
    () => makeReadNotificationsSelector(laoId.valueOf()),
    [laoId],
  );
  const unreadNotifications = useSelector(selectUnreadNotifications);
  const readNotifications = useSelector(selectReadNotifications);

  return (
    <ScreenWrapper>
      <View style={List.container}>
        <NotificationList
          title={STRINGS.notification_unread_notifications}
          notifications={unreadNotifications}
        />
        <NotificationList
          title={STRINGS.notification_read_notifications}
          notifications={readNotifications}
        />
      </View>
    </ScreenWrapper>
  );
};

export default NotificationScreen;

/**
 * Component rendered in the top right of the navigation bar when looking at
 * the notifications screen. Allows the user to perform a set of actions such
 * as clearing the set of all notifications.
 */
export const NotificationScreenRightHeader = () => {
  const showActionSheet = useActionSheet();
  const notificationTypes = NotificationHooks.useNotificationTypes();

  const laoId = NotificationHooks.useAssertCurrentLaoId();

  const selectUnreadNotifications = useMemo(
    () => makeUnreadNotificationsSelector(laoId.valueOf()),
    [laoId],
  );
  const selectReadNotifications = useMemo(
    () => makeReadNotificationsSelector(laoId.valueOf()),
    [laoId],
  );
  const unreadNotifications = useSelector(selectUnreadNotifications);
  const readNotifications = useSelector(selectReadNotifications);

  const onClearNotifications = () => {
    const allNotifications = [...unreadNotifications, ...readNotifications];
    // call custom delete function on all notifications
    for (const notification of allNotifications) {
      const deleteFn = notificationTypes.find((t) => t.isOfType(notification))?.delete;

      // if a delete function was provided for this type, then call it
      if (deleteFn) {
        deleteFn(notification);
      }
    }
    // remove notifications from the notification reducer
    dispatch(
      discardNotifications({
        laoId: laoId.valueOf(),
        notificationIds: allNotifications.map((n) => n.id),
      }),
    );
  };

  return (
    <PoPTouchableOpacity
      onPress={() =>
        showActionSheet([
          { displayName: STRINGS.notification_clear_all, action: onClearNotifications },
        ])
      }>
      <PoPIcon name="options" color={Color.inactive} size={Icon.size} />
    </PoPTouchableOpacity>
  );
};
