import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';

import STRINGS from '../res/strings';
import Witness from '../Components/Witness';
import WitnessVideo from '../Components/WitnessVideo';

/**
* Define the Witness stack navigation
*/

const Stack = createStackNavigator();

export default function WitnessNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name={STRINGS.witness_navigation_tab_home}
        component={Witness}
      />
      <Stack.Screen
        name={STRINGS.witness_navigation_tab_video}
        component={WitnessVideo}
      />
    </Stack.Navigator>
  );
}
