import { useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';

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

  return (
    <ScreenWrapper>
      <Text>{JSON.stringify(notification)}</Text>
    </ScreenWrapper>
  );
};

export default SingleNotificationScreen;
