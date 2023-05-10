import { createStackNavigator } from '@react-navigation/stack';
import React, { useMemo } from 'react';

import ButtonPadding from 'core/components/ButtonPadding';
import { AppScreen } from 'core/navigation/AppNavigation';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { HomeParamList } from 'core/navigation/typing/HomeParamList';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { HomeFeature } from '../interface';
import { Home } from '../screens';
import { HomeHeaderRight } from '../screens/Home';
import ConnectNavigation from './ConnectNavigation';

const homeScreens: HomeFeature.HomeScreen[] = [
  {
    id: STRINGS.navigation_home_home,
    title: STRINGS.home_navigation_title,
    Component: Home,
    headerLeft: () => <ButtonPadding paddingAmount={1} />,
    headerRight: HomeHeaderRight,
  } as HomeFeature.HomeScreen,
  {
    id: STRINGS.navigation_home_connect,
    title: STRINGS.navigation_home_connect,
    Component: ConnectNavigation,
    headerShown: false,
  } as HomeFeature.HomeScreen,
];

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
      ...homeScreens,
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
              headerLeft: headerLeft || stackScreenOptionsWithHeader.headerLeft,
              headerRight: headerRight || stackScreenOptionsWithHeader.headerRight,
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
