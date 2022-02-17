import React from 'react';
import { StyleSheet } from 'react-native';
import { createStackNavigator } from '@react-navigation/stack';
import { SafeAreaView } from 'react-native-safe-area-context';

import STRINGS from 'res/strings';

import LaoNavigation from 'navigation/bars/LaoNavigation';
import MainNavigation from 'navigation/bars/MainNavigation';

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

function AppNavigation() {
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
