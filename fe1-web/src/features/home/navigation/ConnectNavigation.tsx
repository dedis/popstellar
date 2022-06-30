import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { AppScreen } from 'core/navigation/AppNavigation';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { ConnectConfirm, ConnectOpenScan, Launch } from '../screens';

const Stack = createStackNavigator<ConnectParamList>();

export default function ConnectNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerLeftContainerStyle: {
          paddingLeft: Spacing.contentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.contentSpacing,
        },
        headerTitleStyle: Typography.topNavigationHeading,
        headerTitleAlign: 'center',
      }}>
      <Stack.Screen
        name={STRINGS.navigation_connect_scan}
        component={ConnectOpenScan}
        options={{ headerShown: false }}
      />
      <Stack.Screen
        name={STRINGS.navigation_connect_confirm}
        component={ConnectConfirm}
        options={{ title: STRINGS.navigation_connect_confirm_title }}
      />
      <Stack.Screen
        name={STRINGS.navigation_connect_launch}
        component={Launch}
        options={{
          title: STRINGS.navigation_connect_launch_title,
        }}
      />
    </Stack.Navigator>
  );
}

export const ConnectNavigationScreen: AppScreen = {
  id: STRINGS.navigation_app_connect,
  title: STRINGS.navigation_app_connect,
  Component: ConnectNavigation,
};
