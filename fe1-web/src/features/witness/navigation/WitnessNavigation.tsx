import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from 'resources/strings';

import { WitnessCamera, WitnessScreen } from 'features/witness/screens';

/**
 * Define the Witness stack navigation
 * Allows to navigate to the WitnessScreen and the WitnessCamera screen
 */

const Stack = createStackNavigator();

export default function WitnessNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.witness_navigation_tab_home} component={WitnessScreen} />
      <Stack.Screen name={STRINGS.witness_navigation_tab_video} component={WitnessCamera} />
    </Stack.Navigator>
  );
}
