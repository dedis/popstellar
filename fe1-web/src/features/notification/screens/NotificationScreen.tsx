import { useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import {
  FlatList,
  StyleSheet,
  Text,
  TextStyle,
  TouchableOpacity,
  View,
  ViewStyle,
} from 'react-native';
import { Button } from 'react-native-elements';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { dispatch } from 'core/redux';
import { Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { NotificationHooks } from '../hooks';
import { NotificationStackParamList } from '../navigation/NotificationStackParamList';
import {
  discardNotifications,
  makeReadNotificationsSelector,
  makeUnreadNotificationsSelector,
  NotificationState,
} from '../reducer';

interface ListSeparatorItem {
  key: string;
  title: string;
}

interface NotificationItem extends NotificationState {
  key: string;
  isLastItem: boolean;
}

const NotificationScreenStyles = StyleSheet.create({
  notificationItem: {
    paddingTop: 16,
    paddingBottom: 16,
    borderBottomColor: '#000',
    borderBottomWidth: 1,
  } as ViewStyle,

  lastNotificationItem: {
    borderBottomWidth: 0,
  } as ViewStyle,
});

type NavigationProps = StackScreenProps<
  NotificationStackParamList,
  typeof STRINGS.notification_navigation_tab_notifications
>;

const NotificationScreen = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

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

  const notificationData = useMemo(() => {
    const read: NotificationItem[] = unreadNotifications.map((n) => ({
      ...n,
      key: n.id.toString(),
      isLastItem: false,
    }));
    const unread: NotificationItem[] = readNotifications.map((n) => ({
      ...n,
      key: n.id.toString(),
      isLastItem: false,
    }));

    const items: (ListSeparatorItem | NotificationItem)[] = [];
    if (unread.length > 0) {
      // set isLastItem properties
      unread[unread.length - 1].isLastItem = true;

      // the key property has to be unique. all notification keys are numbers so
      // use a string starting with characters for separators
      items.push(
        { key: 'seperator:unread', title: 'Notifications' } as ListSeparatorItem,
        ...unread,
      );
    }

    if (read.length > 0) {
      // set isLastItem properties
      read[read.length - 1].isLastItem = true;

      // the key property has to be unique. all notification keys are numbers so
      // use a string starting with characters for separators
      items.push(
        { key: 'seperator:read', title: 'Read Notifications' } as ListSeparatorItem,
        ...read,
      );
    }

    return items;
  }, [unreadNotifications, readNotifications]);

  const notificationTypes = NotificationHooks.useNotificationTypes();

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
    <ScreenWrapper>
      <FlatList
        data={notificationData}
        keyExtractor={(item) => item.key}
        renderItem={({ item }) => {
          if ('id' in item) {
            // notification
            return (
              <TouchableOpacity
                onPress={() =>
                  navigation.navigate<'Notification'>(
                    STRINGS.notification_navigation_tab_single_notification,
                    { notificationId: item.id },
                  )
                }>
                <View
                  style={
                    item.isLastItem
                      ? [
                          NotificationScreenStyles.notificationItem,
                          NotificationScreenStyles.lastNotificationItem,
                        ]
                      : NotificationScreenStyles.notificationItem
                  }>
                  <Text>{item.title}</Text>
                </View>
              </TouchableOpacity>
            );
          }
          // separator
          return <Text style={Typography.important as TextStyle}>{item.title}</Text>;
        }}
      />
      <Button title={STRINGS.notification_clear_all} onPress={onClearNotifications} />
    </ScreenWrapper>
  );
};

export default NotificationScreen;
