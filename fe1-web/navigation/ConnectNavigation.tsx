import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from 'res/strings';

import ConnectUnapprove from 'components/ConnectUnapprove';
import ConnectScanning from 'components/ConnectScanning';
import ConnectConnecting from 'components/ConnectConnecting';
import ConnectConfirm from 'components/ConnectConfirm';

/**
 * Define the Connect stack navigation
 *
 * Contains four screen:
 *  - The ConnectUnapprove
 *  - The ConnectScanning
 *  - The ConnectConnecting
 *  - The ConnectConfirm
*/

const Stack = createStackNavigator();

export default function ConnectNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name={STRINGS.connect_unapproved_title}
        component={ConnectUnapprove}
      />
      <Stack.Screen
        name={STRINGS.connect_scanning_title}
        component={ConnectScanning}
      />
      <Stack.Screen
        name={STRINGS.connect_connecting_title}
        component={ConnectConnecting}
      />
      <Stack.Screen
        name={STRINGS.connect_confirm_title}
        component={ConnectConfirm}
      />
    </Stack.Navigator>
  );
}
