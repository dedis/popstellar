import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from 'res/strings';

import Launch from '../../parts/launch/Launch';
import LaunchConfirm from '../../parts/launch/LaunchConfirm';

/**
 * Define the launch panel stack navigation
 *
 * Contains two components:
 *  - The LaunchMain (Launch)
 *  - The LaunchConfirm
 */

const Stack = createStackNavigator();

export default function LaunchNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name={STRINGS.launch_navigation_tab_main}
        component={Launch}
      />
      <Stack.Screen
        name={STRINGS.launch_navigation_tab_confirm}
        component={LaunchConfirm}
      />
    </Stack.Navigator>
  );
}
