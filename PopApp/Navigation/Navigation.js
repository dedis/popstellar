import React from 'react';
import { Platform, StyleSheet } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';

import STRINGS from '../res/strings';

import Launch from '../Components/Launch';
import Home from '../Components/Home';
import ConnectNavigation from './ConnectNavigation';

const TopTabNavigator = createMaterialTopTabNavigator();

/**
* The main tab navigation component
*
* create a tab navigator between the Home, Connect and Launch component
*
* the SafeAreaView resolves problem with status bar overlap
*/
const styles = StyleSheet.create({
  view: {
    flex: 1,
  },
  navigator: {
    ...Platform.select({
      web: {
        width: '100vw',
      },
      default: {},
    }),
  },
});

export default function TabNavigation() {
  return (
    <SafeAreaView style={styles.view}>
      <TopTabNavigator.Navigator style={styles.navigator}>
        <TopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={Home} />
        <TopTabNavigator.Screen
          name={STRINGS.navigation_tab_connect}
          component={ConnectNavigation}
        />
        <TopTabNavigator.Screen name={STRINGS.navigation_tab_launch} component={Launch} />
      </TopTabNavigator.Navigator>
    </SafeAreaView>
  );
}
