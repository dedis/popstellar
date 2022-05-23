import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';

import EventIcon from 'core/components/icons/EventIcon';
import HomeIcon from 'core/components/icons/HomeIcon';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import { selectIsLaoOrganizer } from '../reducer';
import { EventsScreen } from '../screens';
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
  const isOrganizer = useSelector(selectIsLaoOrganizer);

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
        tabBarIcon: HomeIcon,
        order: -9999999,
      } as LaoFeature.LaoScreen,
      {
        id: STRINGS.navigation_lao_events,
        tabBarIcon: EventIcon,
        Component: isOrganizer ? EventsNavigation : EventsScreen,
        headerShown: false,
        order: 0,
      } as LaoFeature.LaoScreen,
      // sort screens by order before rendering them
    ].sort((a, b) => a.order - b.order);
  }, [passedScreens, isOrganizer]);

  return (
    <OrganizationTopTabNavigator.Navigator
      initialRouteName={STRINGS.navigation_lao_home}
      screenOptions={{
        tabBarActiveTintColor: Color.accent,
        tabBarInactiveTintColor: Color.inactive,
        headerLeftContainerStyle: {
          paddingLeft: Spacing.horizontalContentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.horizontalContentSpacing,
        },
        headerTitleStyle: Typography.topNavigationHeading,
        headerTitleAlign: 'center',
      }}>
      {screens.map(
        ({ id, title, headerTitle, Component, headerShown, headerRight, tabBarIcon }) => (
          <OrganizationTopTabNavigator.Screen
            key={id}
            name={id}
            component={Component}
            options={{
              title: title || id,
              headerTitle: headerTitle || title || id,
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
