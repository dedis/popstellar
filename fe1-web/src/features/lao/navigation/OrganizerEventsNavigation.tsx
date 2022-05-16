import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { OrganizerScreen } from '../screens';

/**
 * Define the Organizer stack navigation
 * four different screen (OrganizerScreen, CreateEvent, RollCallScanning)
 *
 * The app are not use in the stack order, only organizer to one of the other screen
 */

const Stack = createStackNavigator();

export default function OrganizerEventsNavigation() {
  const screens = LaoHooks.useOrganizerNavigationScreens();

  // sort screens by order before rendering them
  screens.sort((a, b) => a.order - b.order);

  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.organizer_navigation_tab_home} component={OrganizerScreen} />
      {screens.map(({ id, title, Component }) => (
        <Stack.Screen name={id} key={id} component={Component} options={{ title: title || id }} />
      ))}
    </Stack.Navigator>
  );
}
