import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import React, { useMemo } from 'react';

import { makeIcon } from 'core/components/PoPIcon';
import { AppScreen } from 'core/navigation/AppNavigation';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import LaoHomeScreen, {
  LaoHomeScreenHeader,
  LaoHomeScreenHeaderRight,
} from '../screens/LaoHomeScreen';
import EventsNavigation from './EventsNavigation';

const OrganizationTopTabNavigator = createBottomTabNavigator<LaoParamList>();

/**
 * Navigation when connected to a lao
 */

const LaoNavigation: React.FC = () => {
  const passedScreens = LaoHooks.useLaoNavigationScreens();

  // add the organizer or attendee screen depeding on the user
  const screens: LaoFeature.LaoScreen[] = useMemo(() => {
    return [
      ...passedScreens,
      {
        id: STRINGS.navigation_lao_home,
        title: STRINGS.navigation_lao_lao_title,
        headerTitle: LaoHomeScreenHeader,
        Component: LaoHomeScreen,
        headerRight: LaoHomeScreenHeaderRight,
        tabBarIcon: makeIcon('home'),
        order: -9999999,
      } as LaoFeature.LaoScreen,
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
    <OrganizationTopTabNavigator.Navigator
      initialRouteName={STRINGS.navigation_lao_home}
      screenOptions={{
        tabBarActiveTintColor: Color.accent,
        tabBarInactiveTintColor: Color.inactive,
        headerLeftContainerStyle: {
          paddingLeft: Spacing.contentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.contentSpacing,
        },
        headerTitleStyle: Typography.topNavigationHeading,
        headerTitleAlign: 'center',
      }}>
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
        }) => (
          <OrganizationTopTabNavigator.Screen
            key={id}
            name={id}
            component={Component}
            options={{
              title: title || id,
              headerTitle: headerTitle || title || id,
              headerLeft,
              headerRight,
              tabBarIcon: tabBarIcon || undefined,
              // hide the item if tabBarIcon is set to null
              tabBarItemStyle: tabBarIcon === null ? { display: 'none' } : undefined,
              headerShown,
            }}
          />
        ),
      )}
    </OrganizationTopTabNavigator.Navigator>
  );
};

export default LaoNavigation;

export const LaoNavigationAppScreen: AppScreen = {
  id: STRINGS.navigation_app_lao,
  Component: LaoNavigation,
};
