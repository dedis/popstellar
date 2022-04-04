import { useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useSelector, useStore } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';

import { NotificationHooks } from '../hooks';
import { NotificationStackParamList } from '../navigation/NotificationStackParamList';
import { getNotification, makeSelectNotification } from '../reducer';

type NavigationProps = StackScreenProps<NotificationStackParamList, 'Notification'>;

const SingleNotificationScreen = () => {
  const route = useRoute<NavigationProps['route']>();
  const navigation = useNavigation<NavigationProps['navigation']>();
  const { notificationId } = route.params;

  const store = useStore();
  const notification = getNotification(notificationId, store.getState());
  const notificationTypeComponents = NotificationHooks.useNotificationTypeComponents();

  // search the notification type component list for a fitting comonent to render
  // this notification. undefined if there is none
  const Component = useMemo(() => {
    if (notification) {
      return notificationTypeComponents.find((c) => c.isOfType(notification))?.Component;
    }
    return undefined;
  }, [notification, notificationTypeComponents]);

  return (
    <ScreenWrapper>
      {Component && notification ? (
        <Component
          notification={notification}
          navigateToNotificationScreen={() =>
            navigation.navigate('NotificationNavigation Notifications')
          }
        />
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
