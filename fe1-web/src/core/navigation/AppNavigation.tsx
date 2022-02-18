import React from 'react';
import { StyleSheet } from 'react-native';
import { createStackNavigator } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';

import STRINGS from 'resources/strings';
import { LaoNavigation } from 'features/lao/navigation';

import MainNavigation from './MainNavigation';

/**
 * Define the App stack navigation
 * Contains to navigation: the home navigation and the organization navigation
 */

const Stack = createStackNavigator();

const styles = StyleSheet.create({
  view: {
    flex: 1,
  },
});

// FIXME: use opts -- Pierluca, 2022-02-18
function AppNavigation({ opts }) {
  return (
    <SafeAreaView style={styles.view}>
      <Stack.Navigator
        screenOptions={{
          headerShown: false,
        }}>
        <Stack.Screen name={STRINGS.app_navigation_tab_home} component={MainNavigation} />
        <Stack.Screen name={STRINGS.app_navigation_tab_organizer} component={LaoNavigation} />
      </Stack.Navigator>
    </SafeAreaView>
  );
}
export default AppNavigation;
