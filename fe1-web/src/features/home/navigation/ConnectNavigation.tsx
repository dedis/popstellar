import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { AppScreen } from 'core/navigation/AppNavigation';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import STRINGS from 'resources/strings';

import { ConnectConfirm, ConnectOpenScan, Launch } from '../screens';

const Stack = createStackNavigator<ConnectParamList>();

export default function ConnectNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.navigation_connect_scan} component={ConnectOpenScan} />
      <Stack.Screen name={STRINGS.navigation_connect_confirm} component={ConnectConfirm} />
      <Stack.Screen name={STRINGS.navigation_connect_launch} component={Launch} />
    </Stack.Navigator>
  );
}

export const ConnectNavigationScreen: AppScreen = {
  id: STRINGS.navigation_app_connect,
  title: STRINGS.navigation_app_connect,
  Component: ConnectNavigation,
};
