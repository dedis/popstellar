import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';

import { Colors, Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import { selectIsLaoOrganizer, selectIsLaoWitness } from '../reducer';
import { AttendeeEventsScreen, Identity } from '../screens';
import OrganizerEventsNavigation from './OrganizerEventsNavigation';

const OrganizationTopTabNavigator = createBottomTabNavigator();

/**
 * Navigation when connected to a lao
 *
 * Displays the following components:
 *  - Home
 *  - Social Media
 *  - Lao tab (corresponding to user role)
 *  - Identity
 *  - Wallet
 *  - Name of the connected lao (fake link)
 */

const LaoNavigation: React.FC = () => {
  const passedScreens = LaoHooks.useLaoNavigationScreens();

  const isOrganizer = useSelector(selectIsLaoOrganizer);
  const isWitness = useSelector(selectIsLaoWitness);

  // add the organizer or attendee screen depeding on the user
  const screens: LaoFeature.Screen[] = useMemo(() => {
    let Component: React.ComponentType<any>;

    if (isOrganizer || isWitness) {
      Component = OrganizerEventsNavigation;
    } else {
      Component = AttendeeEventsScreen;
    }

    return [
      ...passedScreens,
      {
        id: STRINGS.organization_navigation_tab_identity,
        Component: Identity,
        order: 10000,
      } as LaoFeature.Screen,
      {
        id: STRINGS.organization_navigation_tab_events,
        Component,
        order: 20000,
      } as LaoFeature.Screen,
      // sort screens by order before rendering them
    ].sort((a, b) => a.order - b.order);
  }, [passedScreens, isOrganizer, isWitness]);

  return (
    <OrganizationTopTabNavigator.Navigator
      initialRouteName={STRINGS.organization_navigation_tab_events}
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
      {screens.map(({ id, title, Component, Badge, Icon }) => (
        <OrganizationTopTabNavigator.Screen
          key={id}
          name={id}
          component={Component}
          options={{
            title: title || id,
            headerRight: Badge,
            tabBarIcon: Icon,
          }}
        />
      ))}
    </OrganizationTopTabNavigator.Navigator>
  );
};

export default LaoNavigation;
