import React from 'react'
import { createStackNavigator } from '@react-navigation/stack';
import ConnectUnapprove from '../Components/ConnectUnapprove';
import ConnectScanning from '../Components/ConnectScanning';
import ConnectConnecting from '../Components/ConnectConnecting';
import ConnectConfirm from '../Components/ConnectConfirm';

/*
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
        name="Unapprove"
        component={ConnectUnapprove}
      />
      <Stack.Screen
        name="Scanning"
        component={ConnectScanning}
      />
      <Stack.Screen
        name="Connecting"
        component={ConnectConnecting}
      />
      <Stack.Screen
        name="Confirm"
        component={ConnectConfirm}
      />
    </Stack.Navigator>
  );
}