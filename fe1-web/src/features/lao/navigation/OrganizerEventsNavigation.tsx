import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { LaoOrganizerParamList } from 'core/navigation/typing/LaoOrganizerParamList';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { OrganizerScreen } from '../screens';

/**
 * Define the Organizer stack navigation
 * four different screen (OrganizerScreen, CreateEvent, RollCallScanning)
 *
 * The app are not use in the stack order, only organizer to one of the other screen
 */

const Stack = createStackNavigator<LaoOrganizerParamList>();

export default function OrganizerEventsNavigation() {
  const screens = LaoHooks.useOrganizerNavigationScreens();

  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.navigation_lao_organizer_home} component={OrganizerScreen} />
      {screens.map(({ id, title, Component, headerShown }) => (
        <Stack.Screen
          name={id}
          key={id}
          component={Component}
          options={{ title: title || id, headerShown }}
        />
      ))}
    </Stack.Navigator>
  );
}
