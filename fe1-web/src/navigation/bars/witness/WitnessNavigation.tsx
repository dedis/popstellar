import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from 'res/strings';

import Witness from 'parts/lao/witness/Witness';
import WitnessCamera from 'parts/lao/witness/WitnessCamera';

/**
 * Define the Witness stack navigation
 * Allows to navigate to the Witness and the WitnessCamera screen
 */

const Stack = createStackNavigator();

export default function WitnessNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen name={STRINGS.witness_navigation_tab_home} component={Witness} />
      <Stack.Screen name={STRINGS.witness_navigation_tab_video} component={WitnessCamera} />
    </Stack.Navigator>
  );
}
