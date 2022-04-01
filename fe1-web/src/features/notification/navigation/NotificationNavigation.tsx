import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import STRINGS from 'resources/strings';

import NotificationScreen from '../screens/NotificationScreen';
import SingleNotificationScreen from '../screens/SingleNotificationScreen';
import { NotificationStackParamList } from './NotificationStackParamList';

const NotificationStackNavigator = createStackNavigator<NotificationStackParamList>();

const NotificationNavigation = () => {
  return (
    <NotificationStackNavigator.Navigator
      initialRouteName={STRINGS.notification_navigation_tab_notifications}>
      <NotificationStackNavigator.Screen
        name={STRINGS.notification_navigation_tab_notifications}
        component={NotificationScreen}
        options={{
          // show not back button for this screen
          headerLeft: () => null,
        }}
      />
      <NotificationStackNavigator.Screen
        name={STRINGS.notification_navigation_tab_single_notification}
        component={SingleNotificationScreen}
      />
    </NotificationStackNavigator.Navigator>
  );
};

export default NotificationNavigation;
