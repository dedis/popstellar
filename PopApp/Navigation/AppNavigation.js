import React from 'react';
import { StyleSheet } from 'react-native';
import { createStackNavigator } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';

import STRINGS from '../res/strings';

import Navigation from './Navigation';
import OrganizerNavigation from './OrganizerNavigation';

/**
* Define the App stack navigation
*/

const Stack = createStackNavigator();

const styles = StyleSheet.create({
  view: {
    flex: 1,
  },
});

export default function AppNavigation() {
  return (
    <SafeAreaView style={styles.view}>
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
    </SafeAreaView>
  );
}
