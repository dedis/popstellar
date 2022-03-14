import React from 'react';
import { Platform, StyleSheet } from 'react-native';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';

import STRINGS from 'resources/strings';

import { Home } from '../screens';
import { HomeHooks } from '../hooks';

/**
 * The main tab navigation component. It creates a tab navigator between the Home, Connect, Launch
 * and Wallet components.
 */
const HomeTopTabNavigator = createMaterialTopTabNavigator();

const styles = StyleSheet.create({
  navigator: {
    ...Platform.select({
      web: {
        width: '100vw',
      },
      default: {},
    }),
  },
});

const MainNavigation = () => {
  const screens = HomeHooks.useMainNavigationScreens();

  return (
    <HomeTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.navigation_tab_home}
      screenOptions={{
        swipeEnabled: false,
      }}>
      <HomeTopTabNavigator.Screen name={STRINGS.navigation_tab_home} component={Home} />
      {screens
        .sort((a, b) => a.order - b.order)
        .map(({ name, Component }, idx) => (
          <HomeTopTabNavigator.Screen key={idx.toString()} name={name} component={Component} />
        ))}
    </HomeTopTabNavigator.Navigator>
  );
};

export default MainNavigation;
