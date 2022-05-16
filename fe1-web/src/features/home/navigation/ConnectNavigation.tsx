import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import STRINGS from 'resources/strings';

import { ConnectConfirm, ConnectOpenScan, Launch } from '../screens';

const Stack = createStackNavigator();

export default function ConnectNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.connect_scanning_title} component={ConnectOpenScan} />
      <Stack.Screen name={STRINGS.connect_confirm_title} component={ConnectConfirm} />
      <Stack.Screen name={STRINGS.navigation_tab_launch} component={Launch} />
    </Stack.Navigator>
  );
}
