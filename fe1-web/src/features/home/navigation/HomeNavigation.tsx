import { createStackNavigator } from '@react-navigation/stack';
import React, { useMemo } from 'react';

import { makeIcon } from 'core/components/PoPIcon';
import { AppScreen } from 'core/navigation/AppNavigation';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { HomeFeature } from '../interface';
import { Home } from '../screens';
import ConnectNavigation from './ConnectNavigation';

/**
 * The main tab navigation component. It creates a tab navigator between the Home, Connect, Launch
 * and Wallet components.
 */
const HomeNavigator = createStackNavigator<HomeParamList>();

const HomeNavigation = () => {
  const navigationScreens = HomeHooks.useHomeNavigationScreens();

  const screens: HomeFeature.HomeScreen[] = useMemo(() => {
    return [
      ...navigationScreens,
      // add home screen to the navigation
      {
        id: STRINGS.navigation_home_home,
        title: STRINGS.home_navigation_title,
        Component: Home,
        tabBarIcon: makeIcon('list'),
        headerLeft: () => null,
      } as HomeFeature.HomeScreen,
      {
        id: STRINGS.navigation_home_connect,
        title: STRINGS.navigation_home_connect,
        Component: ConnectNavigation,
      },
      // sort screens by order before rendering them
    ];
  }, [navigationScreens]);

  return (
    <HomeNavigator.Navigator
      initialRouteName={STRINGS.navigation_home_home}
      screenOptions={stackScreenOptionsWithHeader}>
      {screens.map(
        ({ id, title, headerTitle, headerShown, Component, headerLeft, headerRight }) => (
          <HomeNavigator.Screen
            key={id}
            name={id}
            component={Component}
            options={{
              title: title || id,
              headerTitle: headerTitle || title || id,
              headerLeft,
              headerRight,
              // hide the item if tabBarIcon is set to null
              headerShown,
            }}
          />
        ),
      )}
    </HomeNavigator.Navigator>
  );
};

export default HomeNavigation;

export const HomeNavigationScreen: AppScreen = {
  id: STRINGS.navigation_app_home,
  title: STRINGS.navigation_app_home,
  Component: HomeNavigation,
};
