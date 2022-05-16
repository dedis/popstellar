import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { useNavigation } from '@react-navigation/core';
import React, { useMemo } from 'react';

import HomeIcon from 'core/components/icons/HomeIcon';
import ScanIcon from 'core/components/icons/ScanIcon';
import { AppScreen } from 'core/navigation/AppNavigation';
import { Colors, Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { HomeFeature } from '../interface';
import { Home } from '../screens';
import ConnectNavigation from './ConnectNavigation';

/**
 * The main tab navigation component. It creates a tab navigator between the Home, Connect, Launch
 * and Wallet components.
 */
const HomeNavigator = createBottomTabNavigator();

const MainNavigation = () => {
  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

  const navigationScreens = HomeHooks.useMainNavigationScreens();

  const screens: HomeFeature.Screen[] = useMemo(() => {
    return [
      ...navigationScreens,
      // add home screen to the navigation
      {
        id: STRINGS.navigation_tab_home,
        title: STRINGS.navigation_tab_home,
        Component: Home,
        tabBarIcon: HomeIcon,
        order: -99999999,
      } as HomeFeature.Screen,
      {
        id: `mock_${STRINGS.navigation_tab_connect}`,
        title: STRINGS.navigation_tab_connect,
        Component: ConnectNavigation,
        tabPress: (e) => {
          // prevent navigation
          e.preventDefault();
          navigation.navigate(STRINGS.navigation_tab_connect);
        },
        tabBarIcon: ScanIcon,
        order: -10000,
      } as HomeFeature.Screen,
      // sort screens by order before rendering them
    ].sort((a, b) => a.order - b.order);
  }, [navigation, navigationScreens]);

  return (
    <HomeNavigator.Navigator
      initialRouteName={STRINGS.navigation_tab_home}
      screenOptions={{
        tabBarActiveTintColor: Colors.primary,
        tabBarInactiveTintColor: Colors.inactive,
        headerLeftContainerStyle: {
          paddingLeft: Spacing.horizontalContentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.horizontalContentSpacing,
        },
        headerTitleAlign: 'center',
      }}>
      {screens.map(({ id, title, Component, tabBarIcon, tabPress }) => (
        <HomeNavigator.Screen
          key={id}
          name={id}
          component={Component}
          listeners={{ tabPress }}
          options={{
            title: title || id,
            tabBarIcon,
          }}
        />
      ))}
    </HomeNavigator.Navigator>
  );
};

export default MainNavigation;

export const MainNavigationScreen: AppScreen = {
  id: STRINGS.app_navigation_tab_home,
  title: STRINGS.app_navigation_tab_home,
  component: MainNavigation,
};
