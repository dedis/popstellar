import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo } from 'react';

import { makeIcon } from 'core/components/Icon';
import { AppScreen } from 'core/navigation/AppNavigation';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { HomeParamList } from 'core/navigation/typing/HomeParamList';
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
const HomeNavigator = createBottomTabNavigator<HomeParamList>();

type NavigationProps = CompositeScreenProps<
  StackScreenProps<HomeParamList, typeof STRINGS.navigation_home_home>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

const HomeNavigation = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();

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
        order: -99999999,
      } as HomeFeature.HomeScreen,
      {
        id: STRINGS.navigation_home_mock_connect,
        title: STRINGS.navigation_app_connect,
        Component: ConnectNavigation,
        tabPress: (e) => {
          // prevent navigation
          e.preventDefault();
          navigation.navigate(STRINGS.navigation_app_connect);
        },
        tabBarIcon: makeIcon('scan'),
        order: -10000,
      } as HomeFeature.HomeScreen,
      // sort screens by order before rendering them
    ].sort((a, b) => a.order - b.order);
  }, [navigation, navigationScreens]);

  return (
    <HomeNavigator.Navigator
      initialRouteName={STRINGS.navigation_home_home}
      screenOptions={{
        tabBarActiveTintColor: Colors.accent,
        tabBarInactiveTintColor: Colors.inactive,
        headerLeftContainerStyle: {
          paddingLeft: Spacing.horizontalContentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.horizontalContentSpacing,
        },
        headerTitleAlign: 'center',
      }}>
      {screens.map(({ id, title, Component, tabBarIcon, tabPress, headerLeft, headerRight }) => (
        <HomeNavigator.Screen
          key={id}
          name={id}
          component={Component}
          listeners={{ tabPress }}
          options={{
            title: title || id,
            tabBarIcon,
            headerLeft,
            headerRight,
          }}
        />
      ))}
    </HomeNavigator.Navigator>
  );
};

export default HomeNavigation;

export const HomeNavigationScreen: AppScreen = {
  id: STRINGS.navigation_app_home,
  title: STRINGS.navigation_app_home,
  component: HomeNavigation,
};
