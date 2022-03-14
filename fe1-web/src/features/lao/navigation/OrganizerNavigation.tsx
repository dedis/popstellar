import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from 'resources/strings';

import { OrganizerScreen } from '../screens';
import { LaoHooks } from '../hooks';

/**
 * Define the Organizer stack navigation
 * four different screen (OrganizerScreen, CreateEvent, RollCallScanning)
 *
 * The app are not use in the stack order, only organizer to one of the other screen
 */

const Stack = createStackNavigator();

export default function OrganizerNavigation() {
  const screens = LaoHooks.useOrganizerNavigationScreens();

  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.organizer_navigation_tab_home} component={OrganizerScreen} />
      {screens
        .sort((a, b) => a.order - b.order)
        .map(({ name, Component }) => (
          <Stack.Screen name={name} key={name} component={Component} />
        ))}
    </Stack.Navigator>
  );
}
