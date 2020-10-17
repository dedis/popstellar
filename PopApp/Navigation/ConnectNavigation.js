import React from 'react'
import { createStackNavigator } from '@react-navigation/stack';
import ConnectUnapprove from '../Components/ConnectUnapprove';
import ConnectScanning from '../Components/ConnectScanning';
import ConnectConnecting from '../Components/ConnectConnecting';
import ConnectConfirm from '../Components/ConnectConfirm';
import STRINGS from '../res/strings';

/**
* Define the Connect stack navigation
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