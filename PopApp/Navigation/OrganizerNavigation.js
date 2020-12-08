import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import Organizer from '../Components/Organizer';
import CreateEvent from '../Components/CreateEvent';
import WitnessScanning from '../Components/WitnessScanning';
import RollCallScanning from '../Components/RollCallScanning';
import STRINGS from '../res/strings';

/**
 * Define the Organizer stack navigation
 * four different screen (Organizer, CreateEvent, WitnessScanning, RollCallScanning)
 *
 * The app are not use in the stack order, only organizer to one of the other screen
*/

const Stack = createStackNavigator();

export default function OrganizerNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name={STRINGS.organizer_navigation_tab_home}
        component={Organizer}
      />
      <Stack.Screen
        name={STRINGS.organizer_navigation_tab_create_event}
        component={CreateEvent}
      />
      <Stack.Screen
        name={STRINGS.organizer_navigation_tab_add_witness}
        component={WitnessScanning}
      />
      <Stack.Screen
        name={STRINGS.organizer_navigation_tab_roll_call}
        component={RollCallScanning}
      />
    </Stack.Navigator>
  );
}
