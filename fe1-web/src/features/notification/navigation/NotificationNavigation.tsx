import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { NotificationParamList } from 'core/navigation/typing/NotificationParamList';
import STRINGS from 'resources/strings';

import { NotificationFeature } from '../interface/Feature';
import NotificationScreen, { NotificationScreenRightHeader } from '../screens/NotificationScreen';
import SingleNotificationScreen from '../screens/SingleNotificationScreen';

const NotificationStackNavigator = createStackNavigator<NotificationParamList>();

const NotificationNavigation = () => {
  return (
    <NotificationStackNavigator.Navigator
      initialRouteName={STRINGS.navigation_notification_notifications}
      screenOptions={stackScreenOptionsWithHeader}>
      <NotificationStackNavigator.Screen
        name={STRINGS.navigation_notification_notifications}
        component={NotificationScreen}
        options={{
          title: STRINGS.navigation_notification_notifications_title,
          headerRight: NotificationScreenRightHeader,
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

export const NotificationNavigationScreen: NotificationFeature.LaoScreen = {
  id: STRINGS.navigation_lao_notifications,
  Component: NotificationNavigation,
  headerShown: false,
  tabBarIcon: null,
  order: 0,
};
