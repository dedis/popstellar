import { useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';

import { NotificationHooks } from '../hooks';
import { NotificationStackParamList } from '../navigation/NotificationStackParamList';
import { makeSelectNotification } from '../reducer';

type NavigationProps = StackScreenProps<NotificationStackParamList, 'Notification'>;

const SingleNotificationScreen = () => {
  const route = useRoute<NavigationProps['route']>();
  const { notificationId } = route.params;

  const selectNotification = useMemo(
    () => makeSelectNotification(notificationId),
    [notificationId],
  );
  const notification = useSelector(selectNotification);
  const notificationTypeComponents = NotificationHooks.useNotificationTypeComponents();

  // search the notification type component list for a fitting comonent to render
  // this notification. undefined if there is none
  const Component = useMemo(() => {
    return notificationTypeComponents.find((c) => c.isOfType(notification))?.Component;
  }, [notification, notificationTypeComponents]);

  return (
    <ScreenWrapper>
      {Component ? (
        <Component notification={notification} />
      ) : (
        <Text>
          <Text>
            No notification type component has been provided to render the following notification:
          </Text>
          <Text>{JSON.stringify(notification)}</Text>
        </Text>
      )}
    </ScreenWrapper>
  );
};

export default SingleNotificationScreen;
