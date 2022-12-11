import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { getFocusedRouteNameFromRoute } from '@react-navigation/core';
import React, { useMemo } from 'react';
import { StyleSheet, Text, View } from 'react-native';

import { makeIcon } from 'core/components/PoPIcon';
import { AppScreen } from 'core/navigation/AppNavigation';
import { tabNavigationOptions } from 'core/navigation/ScreenOptions';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import NoCurrentLaoErrorBoundary from '../errors/NoCurrentLaoErrorBoundary';
import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import EventsNavigation from './EventsNavigation';

const OrganizationBottomTabNavigator = createBottomTabNavigator<LaoParamList>();

const styles = StyleSheet.create({
  offlineHeader: {
    width: '100%',
    backgroundColor: Color.warning,
    paddingVertical: Spacing.x05,
    paddingHorizontal: Spacing.contentSpacing,
  },
});

const OfflineHeader = () => {
  const isConnected = LaoHooks.useConnectedToLao();

  if (isConnected) {
    return null;
  }

  return (
    <View style={styles.offlineHeader}>
      <Text style={[Typography.base, Typography.small, Typography.centered, Typography.negative]}>
        {STRINGS.general_offline}
      </Text>
    </View>
  );
};

const wrapWithOfflineHeader = (Component: React.ComponentType<unknown>) => () => {
  return (
    <>
      <Component />
      <OfflineHeader />
    </>
  );
};

/**
 * Navigation when connected to a lao
 */
const LaoNavigation: React.FC<unknown> = () => {
  const passedScreens = LaoHooks.useLaoNavigationScreens();

  // add the organizer or attendee screen depeding on the user
  const screens: LaoFeature.LaoScreen[] = useMemo(() => {
    return (
      [
        ...passedScreens,
        {
          id: STRINGS.navigation_lao_events,
          tabBarIcon: makeIcon('event'),
          Component: EventsNavigation,
          headerShown: false,
          order: 0,
          tabBarVisible: (routeName) =>
            // only show the tab bar if we are on the home events screen, not if we are
            // in a detail screen
            routeName === undefined || routeName === STRINGS.navigation_lao_events_home,
        } as LaoFeature.LaoScreen,
        // sort screens by order before rendering them
      ]
        .sort((a, b) => a.order - b.order)
        // wrap each screen component
        .map((s) => ({ ...s, Component: wrapWithOfflineHeader(s.Component) }))
    );
  }, [passedScreens]);

  return (
    <NoCurrentLaoErrorBoundary>
      <OrganizationBottomTabNavigator.Navigator
        initialRouteName={STRINGS.navigation_lao_events}
        screenOptions={tabNavigationOptions}>
        {screens.map(
          ({
            id,
            title,
            headerTitle,
            Component,
            headerShown,
            headerLeft,
            headerRight,
            tabBarIcon,
            tabBarVisible,
            testID,
          }) => (
            <OrganizationBottomTabNavigator.Screen
              key={id}
              name={id}
              component={Component}
              options={({ route }) => {
                const routeName = getFocusedRouteNameFromRoute(route);

                return {
                  title: title || id,
                  headerTitle: headerTitle || title || id,
                  headerLeft: headerLeft || tabNavigationOptions.headerLeft,
                  headerRight: headerRight || tabNavigationOptions.headerRight,
                  tabBarIcon: tabBarIcon || undefined,
                  // hide the item if tabBarIcon is set to null
                  tabBarItemStyle: tabBarIcon === null ? { display: 'none' } : undefined,
                  headerShown,
                  tabBarTestID: testID,
                  tabBarStyle:
                    tabBarVisible && !tabBarVisible(routeName) ? { display: 'none' } : undefined,
                };
              }}
            />
          ),
        )}
      </OrganizationBottomTabNavigator.Navigator>
    </NoCurrentLaoErrorBoundary>
  );
};

export default LaoNavigation;

export const LaoNavigationAppScreen: AppScreen = {
  id: STRINGS.navigation_app_lao,
  Component: LaoNavigation,
};
