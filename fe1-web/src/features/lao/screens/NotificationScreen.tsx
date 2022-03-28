import React, { useMemo } from 'react';
import { Text, TextStyle } from 'react-native';
import { Button } from 'react-native-elements';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { Timestamp } from 'core/objects';
import { dispatch } from 'core/redux';
import { Typography } from 'core/styles';

import { addNotification, selectAllNotifications } from '../reducer';

const NotificationScreen = () => {
  const notifications = useSelector(selectAllNotifications);
  const [readNotifications, unreadNotifications] = useMemo(() => {
    const read = [];
    const unread = [];
    for (const notification of notifications) {
      if (notification.hasBeenRead) {
        read.push(notification);
      } else {
        unread.push(notification);
      }
    }

    // sort in descending order, i.e. newest/latest first
    read.sort((a, b) => b.timestamp - a.timestamp);
    unread.sort((a, b) => b.timestamp - a.timestamp);

    return [read, unread];
  }, [notifications]);

  return (
    <ScreenWrapper>
      <Text style={Typography.important as TextStyle}>Notifications</Text>
      {unreadNotifications.map((notification) => (
        <Text>{notification.type}</Text>
      ))}

      <Text style={Typography.important as TextStyle}>Read Notifications</Text>
      {readNotifications.map((notification) => (
        <Text>{notification.type}</Text>
      ))}
    </ScreenWrapper>
  );
};

export default NotificationScreen;
