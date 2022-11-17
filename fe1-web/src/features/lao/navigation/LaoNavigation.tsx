import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import React, { useMemo } from 'react';

import { makeIcon } from 'core/components/PoPIcon';
import { AppScreen } from 'core/navigation/AppNavigation';
import { tabNavigationOptions } from 'core/navigation/ScreenOptions';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import STRINGS from 'resources/strings';

import NoCurrentLaoErrorBoundary from '../errors/NoCurrentLaoErrorBoundary';
import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import EventsNavigation from './EventsNavigation';

const OrganizationTopTabNavigator = createBottomTabNavigator<LaoParamList>();

/**
 * Navigation when connected to a lao
 */

const LaoNavigation: React.FC<unknown> = () => {
  const passedScreens = LaoHooks.useLaoNavigationScreens();

  // add the organizer or attendee screen depeding on the user
  const screens: LaoFeature.LaoScreen[] = useMemo(() => {
    return [
      ...passedScreens,
      {
        id: STRINGS.navigation_lao_events,
        tabBarIcon: makeIcon('event'),
        Component: EventsNavigation,
        headerShown: false,
        order: 0,
      } as LaoFeature.LaoScreen,
      // sort screens by order before rendering them
    ].sort((a, b) => a.order - b.order);
  }, [passedScreens]);

  return (
    <NoCurrentLaoErrorBoundary>
      <OrganizationTopTabNavigator.Navigator
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
            testID,
          }) => (
            <OrganizationTopTabNavigator.Screen
              key={id}
              name={id}
              component={Component}
              options={{
                title: title || id,
                headerTitle: headerTitle || title || id,
                headerLeft: headerLeft || tabNavigationOptions.headerLeft,
                headerRight,
                tabBarIcon: tabBarIcon || undefined,
                // hide the item if tabBarIcon is set to null
                tabBarItemStyle: tabBarIcon === null ? { display: 'none' } : undefined,
                headerShown,
                tabBarTestID: testID,
              }}
            />
          ),
        )}
      </OrganizationTopTabNavigator.Navigator>
    </NoCurrentLaoErrorBoundary>
  );
};

export default LaoNavigation;

export const LaoNavigationAppScreen: AppScreen = {
  id: STRINGS.navigation_app_lao,
  Component: LaoNavigation,
};
