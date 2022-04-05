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
import { discardNotifications, NotificationState, selectAllNotifications } from '../reducer';

interface ListSeparatorItem {
  title: string;
}

interface NotificationItem extends NotificationState {
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
  'NotificationNavigation Notifications'
>;

const NotificationScreen = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

  const notifications = useSelector(selectAllNotifications);
  const notificationData = useMemo(() => {
    const read: NotificationItem[] = [];
    const unread: NotificationItem[] = [];
    for (const notification of notifications) {
      if (notification.hasBeenRead) {
        read.push({ ...notification, isLastItem: false });
      } else {
        unread.push({ ...notification, isLastItem: false });
      }
    }

    // sort in descending order, i.e. newest/latest first
    read.sort((a, b) => b.timestamp - a.timestamp);
    unread.sort((a, b) => b.timestamp - a.timestamp);

    const items: (ListSeparatorItem | NotificationItem)[] = [];
    if (unread.length > 0) {
      // set isLastItem properties
      unread[unread.length - 1].isLastItem = true;

      items.push({ title: 'Notifications' } as ListSeparatorItem, ...unread);
    }

    if (read.length > 0) {
      // set isLastItem properties
      read[read.length - 1].isLastItem = true;

      items.push({ title: 'Read Notifications' } as ListSeparatorItem, ...read);
    }

    return items;
  }, [notifications]);

  const notificationTypes = NotificationHooks.useNotificationTypes();

  const onClearNotifications = () => {
    // call custom delete function on all notifications
    for (const notification of notifications) {
      const deleteFn = notificationTypes.find((t) => t.isOfType(notification))?.delete;

      // if a delete function was provided for this type, then call it
      if (deleteFn) {
        deleteFn(notification);
      }
    }
    // remove notifications from the notification reducer
    dispatch(discardNotifications(notifications.map((n) => n.id)));
  };

  return (
    <ScreenWrapper>
      <FlatList
        data={notificationData}
        keyExtractor={(item) => ('id' in item ? item.id.toString() : item.title)}
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
      <Button title="Clear notifications" onPress={onClearNotifications} />
    </ScreenWrapper>
  );
};

export default NotificationScreen;
