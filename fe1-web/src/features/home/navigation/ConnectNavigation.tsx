import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import STRINGS from 'resources/strings';

import { ConnectConfirm, ConnectOpenScan, Launch } from '../screens';

const Stack = createStackNavigator<ConnectParamList>();

export default function ConnectNavigation() {
  return (
    <Stack.Navigator screenOptions={stackScreenOptionsWithHeader}>
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
