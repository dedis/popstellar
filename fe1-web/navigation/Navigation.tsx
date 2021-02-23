import React from 'react';
import { Platform, StyleSheet } from 'react-native';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';

import STRINGS from 'res/strings';

import Launch from 'components/Launch';
import Home from 'parts/Home';

import ConnectNavigation from './ConnectNavigation';

/**
* The main tab navigation component
*
* create a tab navigator between the Home, ConnectEnableCamera and Launch component
*/
const HomeTopTabNavigator = createMaterialTopTabNavigator();

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

export default function Navigation() {
  return (
    <HomeTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.navigation_tab_home}
    >
      <HomeTopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={Home} />
      <HomeTopTabNavigator.Screen
        name={STRINGS.navigation_tab_connect}
        component={ConnectNavigation}
      />
      <HomeTopTabNavigator.Screen name={STRINGS.navigation_tab_launch} component={Launch} />
    </HomeTopTabNavigator.Navigator>
  );
}
