import { useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo, useState } from 'react';
import { View } from 'react-native';
import { ListItem } from 'react-native-elements';
import { TouchableOpacity } from 'react-native-gesture-handler';
import { useSelector } from 'react-redux';
import ReactTimeago from 'react-timeago';

import OptionsIcon from 'core/components/icons/OptionsIcon';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { NotificationParamList } from 'core/navigation/typing/NotificationParamList';
import { dispatch } from 'core/redux';
import { Color, Icon, List } from 'core/styles';
import STRINGS from 'resources/strings';

import { NotificationHooks } from '../hooks';
import {
  discardNotifications,
  makeReadNotificationsSelector,
  makeUnreadNotificationsSelector,
} from '../reducer';

type NavigationProps = StackScreenProps<
  NotificationParamList,
  typeof STRINGS.navigation_notification_notifications
>;

/**
 * The notification screen component displaying the list of read and unread notifications
 */
const NotificationScreen = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const laoId = NotificationHooks.useCurrentLaoId();

  const [showUnreadNotification, setShowUnreadNotification] = useState(true);
  const [showReadNotification, setShowReadNotification] = useState(false);

  const notificationTypes = NotificationHooks.useNotificationTypes();

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
        <ListItem.Accordion
          containerStyle={List.item}
          content={
            <ListItem.Content>
              <ListItem.Title>Notifications</ListItem.Title>
            </ListItem.Content>
          }
          onPress={() => setShowUnreadNotification(!showUnreadNotification)}
          isExpanded={showUnreadNotification}>
          {unreadNotifications.map((notification) => {
            const NotificationType = notificationTypes.find((t) => t.isOfType(notification));

            if (!NotificationType) {
              console.error('Unregistered notification type', notification);
              throw new Error('Unregistered notification type');
            }

            return (
              <ListItem
                key={notification.id}
                containerStyle={List.item}
                onPress={() =>
                  navigation.navigate<'Notification'>(
                    STRINGS.navigation_notification_single_notification,
                    {
                      notificationId: notification.id,
                    },
                  )
                }>
                <View style={List.icon}>
                  <NotificationType.Icon size={Icon.size} color={Color.primary} />
                </View>
                <ListItem.Content>
                  <ListItem.Title>{notification.title}</ListItem.Title>
                  <ListItem.Subtitle>
                    <ReactTimeago date={notification.timestamp * 1000} />
                  </ListItem.Subtitle>
                </ListItem.Content>
              </ListItem>
            );
          })}
        </ListItem.Accordion>
        <ListItem.Accordion
          containerStyle={List.item}
          content={
            <ListItem.Content>
              <ListItem.Title>Read Notifications</ListItem.Title>
            </ListItem.Content>
          }
          onPress={() => setShowReadNotification(!showReadNotification)}
          isExpanded={showReadNotification}>
          {readNotifications.map((notification) => (
            <ListItem
              key={notification.id}
              containerStyle={List.item}
              onPress={() =>
                navigation.navigate<'Notification'>(
                  STRINGS.navigation_notification_single_notification,
                  {
                    notificationId: notification.id,
                  },
                )
              }>
              <View style={List.iconPlaceholder} />
              <ListItem.Content>
                <ListItem.Title>{notification.title}</ListItem.Title>
                <ListItem.Subtitle>
                  <ReactTimeago date={notification.timestamp} />
                </ListItem.Subtitle>
              </ListItem.Content>
            </ListItem>
          ))}
        </ListItem.Accordion>
      </View>
    </ScreenWrapper>
  );
};

export default NotificationScreen;

export const NotificationScreenRightHeader = () => {
  const showActionSheet = useActionSheet();
  const notificationTypes = NotificationHooks.useNotificationTypes();

  const laoId = NotificationHooks.useCurrentLaoId();

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
    <TouchableOpacity
      onPress={() =>
        showActionSheet([
          { displayName: STRINGS.notification_clear_all, action: onClearNotifications },
        ])
      }>
      <OptionsIcon color={Color.inactive} size={Icon.size} />
    </TouchableOpacity>
  );
};
