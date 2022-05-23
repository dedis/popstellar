import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { LaoEventsParamList } from 'core/navigation/typing/LaoOrganizerParamList';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { EventsScreen } from '../screens';

/**
 * Define the Organizer stack navigation
 * four different screen (OrganizerScreen, CreateEvent, RollCallScanning)
 *
 * The app are not use in the stack order, only organizer to one of the other screen
 */

const Stack = createStackNavigator<LaoEventsParamList>();

export default function OrganizerEventsNavigation() {
  const screens = LaoHooks.useEventsNavigationScreens();

  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.navigation_lao_events_home} component={EventsScreen} />
      {screens.map(({ id, title, Component }) => (
        <Stack.Screen name={id} key={id} component={Component} options={{ title: title || id }} />
      ))}
    </Stack.Navigator>
  );
}
