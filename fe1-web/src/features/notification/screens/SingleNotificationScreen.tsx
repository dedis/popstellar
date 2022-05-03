import { useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useStore } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import STRINGS from 'resources/strings';

import { NotificationHooks } from '../hooks';
import { NotificationStackParamList } from '../navigation/NotificationStackParamList';
import { getNotification } from '../reducer';

type NavigationProps = StackScreenProps<
  NotificationStackParamList,
  typeof STRINGS.notification_navigation_tab_single_notification
>;

const SingleNotificationScreen = () => {
  const route = useRoute<NavigationProps['route']>();
  const navigation = useNavigation<NavigationProps['navigation']>();
  const { notificationId } = route.params;

  const store = useStore();
  const laoId = NotificationHooks.useCurrentLaoId();
  const notification = getNotification(laoId.valueOf(), notificationId, store.getState());
  const notificationTypes = NotificationHooks.useNotificationTypes();

  // search the notification type component list for a fitting comonent to render
  // this notification. undefined if there is none
  const Component = useMemo(() => {
    if (notification) {
      return notificationTypes.find((c) => c.isOfType(notification))?.Component;
    }
    return undefined;
  }, [notification, notificationTypes]);

  return (
    <ScreenWrapper>
      {Component && notification ? (
        <Component
          notification={notification}
          navigateToNotificationScreen={() => navigation.goBack()}
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
