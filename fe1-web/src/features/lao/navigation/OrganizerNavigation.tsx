import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { CreateEvent } from 'features/events/screens';
import { CreateElection } from 'features/evoting/screens';
import { CreateMeeting } from 'features/meeting/screens';
import { CreateRollCall, RollCallOpened } from 'features/rollCall/screens';
import STRINGS from 'resources/strings';

import { OrganizerScreen } from '../screens';

/**
 * Define the Organizer stack navigation
 * four different screen (OrganizerScreen, CreateEvent, RollCallScanning)
 *
 * The app are not use in the stack order, only organizer to one of the other screen
 */

const Stack = createStackNavigator();

export default function OrganizerNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.organizer_navigation_tab_home} component={OrganizerScreen} />
      <Stack.Screen name={STRINGS.organizer_navigation_tab_create_event} component={CreateEvent} />
      <Stack.Screen
        name={STRINGS.organizer_navigation_creation_meeting}
        component={CreateMeeting}
      />
      <Stack.Screen
        name={STRINGS.organizer_navigation_creation_roll_call}
        component={CreateRollCall}
      />
      <Stack.Screen
        name={STRINGS.organizer_navigation_creation_election}
        component={CreateElection}
      />
      <Stack.Screen name={STRINGS.roll_call_open} component={RollCallOpened} />
    </Stack.Navigator>
  );
}
