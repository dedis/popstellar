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

/**
 * Renders a single item in the list of notifications which can either be
 * a notification or a heading separating different sets of notifications
 */
const NotificationScreenListItem = ({ item }: { item: ListSeparatorItem | NotificationItem }) => {
  const navigation = useNavigation<NavigationProps['navigation']>();

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
  return <Text style={Typography.important}>{item.title}</Text>;
};

/**
 * The notification screen component displaying the list of read and unread notifications
 */
const NotificationScreen = () => {
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

  // prepare the list of notifications for passing it to a FlatList component
  // by adding properties for styling as well as adding headings in betweeen
  const notificationData = useMemo(() => {
    const addKey = (n: NotificationState): NotificationItem => ({
      ...n,
      key: n.id.toString(),
      isLastItem: false,
    });

    const read: NotificationItem[] = unreadNotifications.map(addKey);
    const unread: NotificationItem[] = readNotifications.map(addKey);

    // these will be the items inside the FlatList component
    const items: (ListSeparatorItem | NotificationItem)[] = [];

    // add heading before the unread notifications if there are any
    if (unread.length > 0) {
      // set isLastItem property that allows having different styles
      unread[unread.length - 1].isLastItem = true;

      // the key property has to be unique. all notification keys are numbers so
      // use a string starting with characters for separators
      items.push(
        { key: 'seperator:unread', title: 'Notifications' } as ListSeparatorItem,
        ...unread,
      );
    }

    // add heading before the read notifications if there are any
    if (read.length > 0) {
      // set isLastItem property that allows having different styles
      read[read.length - 1].isLastItem = true;

      // the key property has to be unique. all notification keys are numbers so
      // use a string starting with characters for separators
      items.push(
        { key: 'seperator:read', title: 'Read Notifications' } as ListSeparatorItem,
        ...read,
      );
    }

    if (new Set(items.map((i) => i.key)).size !== items.length) {
      console.debug('items:', items);
      throw new Error(
        'The keys of the items passed to the FlatList in NotificationScreen do not have unique keys!',
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
        renderItem={({ item }) => <NotificationScreenListItem item={item} />}
      />
      <Button title={STRINGS.notification_clear_all} onPress={onClearNotifications} />
    </ScreenWrapper>
  );
};

export default NotificationScreen;
