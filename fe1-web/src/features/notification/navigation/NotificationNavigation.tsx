import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { NotificationParamList } from 'core/navigation/typing/NotificationParamList';
import STRINGS from 'resources/strings';

import NotificationScreen from '../screens/NotificationScreen';
import SingleNotificationScreen from '../screens/SingleNotificationScreen';

const NotificationStackNavigator = createStackNavigator<NotificationParamList>();

const NotificationNavigation = () => {
  return (
    <NotificationStackNavigator.Navigator
      initialRouteName={STRINGS.navigation_notification_notifications}>
      <NotificationStackNavigator.Screen
        name={STRINGS.navigation_notification_notifications}
        component={NotificationScreen}
        options={{
          title: STRINGS.navigation_notification_notifications_title,
          // show no back button for this screen in the top navigation bar
          // the back button should only be used from notification detail/single views
          // to get back to this screen but not to go from this screen "back" to anywhere
          headerLeft: () => null,
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
