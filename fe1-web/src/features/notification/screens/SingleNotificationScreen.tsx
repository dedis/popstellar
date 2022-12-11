import { useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';
import { Text } from 'react-native';
import { useSelector } from 'react-redux';

import ScreenWrapper from 'core/components/ScreenWrapper';
import { NotificationParamList } from 'core/navigation/typing/NotificationParamList';
import STRINGS from 'resources/strings';

import { NotificationHooks } from '../hooks';
import { makeNotificationSelector } from '../reducer';

type NavigationProps = StackScreenProps<
  NotificationParamList,
  typeof STRINGS.navigation_notification_single_notification
>;

const SingleNotificationScreen = () => {
  const route = useRoute<NavigationProps['route']>();
  const navigation = useNavigation<NavigationProps['navigation']>();
  const { notificationId } = route.params;

  const laoId = NotificationHooks.useCurrentLaoId();

  const notificationSelector = useMemo(
    () => makeNotificationSelector(laoId, notificationId),
    [laoId, notificationId],
  );

  const notificationState = useSelector(notificationSelector);
  const notificationTypes = NotificationHooks.useNotificationTypes();

  // search the notification type component list for a fitting comonent to render
  // this notification. undefined if there is none
  const { notification, Component } = useMemo(() => {
    if (notificationState) {
      const notificationType = notificationTypes.find((c) => c.isOfType(notificationState));
      if (!notificationType) {
        throw new Error(`Unkown notification type ${notificationState.type}`);
      }

      return {
        notification: notificationType.fromState(notificationState),
        Component: notificationType.Component,
      };
    }
    return {
      notification: undefined,
      Component: undefined,
    };
  }, [notificationState, notificationTypes]);

  return (
    <ScreenWrapper>
      {notification && Component && notificationState ? (
        <Component
          notification={notification}
          navigateToNotificationScreen={() => navigation.goBack()}
        />
      ) : (
        <Text>
          Unkown notification type &apos{notificationState?.type}&apos. Please report this as a bug.
        </Text>
      )}
    </ScreenWrapper>
  );
};

export default SingleNotificationScreen;
