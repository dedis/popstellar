import React from 'react'
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from '../res/strings';

import Navigation from './Navigation'
import OrganizerNavigation from './OrganizerNavigation'


/**
* Define the App stack navigation
*/

const Stack = createStackNavigator();

export default function AppNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name={STRINGS.app_navigation_tab_home}
        component={Navigation}
      />
      <Stack.Screen
        name={STRINGS.app_navigation_tab_organizer}
        component={OrganizerNavigation}
      />
    </Stack.Navigator>
  );
}