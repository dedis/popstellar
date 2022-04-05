import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import React, { useMemo } from 'react';
import { Platform, StyleSheet } from 'react-native';

import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { HomeFeature } from '../interface';
import { Home, Launch } from '../screens';

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
  const navigationScreens = HomeHooks.useMainNavigationScreens();

  const screens: HomeFeature.Screen[] = useMemo(() => {
    return [
      ...navigationScreens,
      // add home screen to the navigation
      {
        id: STRINGS.navigation_tab_home,
        title: STRINGS.navigation_tab_home,
        Component: Home,
        order: -99999999,
      } as HomeFeature.Screen,
      // add launch screen to the navigation
      {
        id: STRINGS.navigation_tab_launch,
        title: STRINGS.navigation_tab_launch,
        Component: Launch,
        order: -1000,
      } as HomeFeature.Screen,
      // sort screens by order before rendering them
    ].sort((a, b) => a.order - b.order);
  }, [navigationScreens]);

  return (
    <HomeTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.navigation_tab_home}
      screenOptions={{
        swipeEnabled: false,
      }}>
      {screens.map(({ id, title, Component }) => (
        <HomeTopTabNavigator.Screen
          key={id}
          name={id}
          component={Component}
          options={{ title: title || id }}
        />
      ))}
    </HomeTopTabNavigator.Navigator>
  );
};

export default MainNavigation;
