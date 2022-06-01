import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import React, { useMemo } from 'react';
import { useSelector } from 'react-redux';

import { makeIcon } from 'core/components/Icon';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { Colors, Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import { selectIsLaoOrganizer, selectIsLaoWitness } from '../reducer';
import { AttendeeEventsScreen, Identity } from '../screens';
import OrganizerEventsNavigation from './OrganizerEventsNavigation';

const OrganizationTopTabNavigator = createBottomTabNavigator<LaoParamList>();

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
  const screens: LaoFeature.LaoScreen[] = useMemo(() => {
    let Component: React.ComponentType<any>;

    if (isOrganizer || isWitness) {
      Component = OrganizerEventsNavigation;
    } else {
      Component = AttendeeEventsScreen;
    }

    return [
      ...passedScreens,
      {
        id: STRINGS.navigation_lao_identity,
        Component: Identity,
        tabBarIcon: makeIcon('identity'),
        order: 10000,
      } as LaoFeature.LaoScreen,
      {
        id: STRINGS.navigation_lao_events,
        Component,
        tabBarIcon: makeIcon('event'),
        order: 20000,
      } as LaoFeature.LaoScreen,
      // sort screens by order before rendering them
    ].sort((a, b) => a.order - b.order);
  }, [passedScreens, isOrganizer, isWitness]);

  return (
    <OrganizationTopTabNavigator.Navigator
      initialRouteName={STRINGS.navigation_lao_events}
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
      {screens.map(({ id, title, Component, headerRight, tabBarIcon }) => (
        <OrganizationTopTabNavigator.Screen
          key={id}
          name={id}
          component={Component}
          options={{
            title: title || id,
            headerRight,
            tabBarIcon,
          }}
        />
      ))}
    </OrganizationTopTabNavigator.Navigator>
  );
};

export default LaoNavigation;
