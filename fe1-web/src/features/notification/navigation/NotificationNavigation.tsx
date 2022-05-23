import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { NotificationParamList } from 'core/navigation/typing/NotificationParamList';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import NotificationScreen from '../screens/NotificationScreen';
import SingleNotificationScreen from '../screens/SingleNotificationScreen';

const NotificationStackNavigator = createStackNavigator<NotificationParamList>();

const NotificationNavigation = () => {
  return (
    <NotificationStackNavigator.Navigator
      initialRouteName={STRINGS.navigation_notification_notifications}
      screenOptions={{
        headerLeftContainerStyle: {
          paddingLeft: Spacing.horizontalContentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.horizontalContentSpacing,
        },
        headerTitleStyle: Typography.topNavigationHeading,
        headerTitleAlign: 'center',
      }}>
      <NotificationStackNavigator.Screen
        name={STRINGS.navigation_notification_notifications}
        component={NotificationScreen}
        options={{
          title: STRINGS.navigation_notification_notifications_title,
        }}
      />
      <NotificationStackNavigator.Screen
        name={STRINGS.navigation_notification_single_notification}
        component={SingleNotificationScreen}
      />
    </NotificationStackNavigator.Navigator>
  );
};

export default NotificationNavigation;
