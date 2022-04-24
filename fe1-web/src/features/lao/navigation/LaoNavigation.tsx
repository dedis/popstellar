import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import React, { useMemo } from 'react';
import { Platform, StyleSheet } from 'react-native';
import { useSelector } from 'react-redux';

import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import { LaoFeature } from '../interface';
import { selectIsLaoOrganizer, selectIsLaoWitness } from '../reducer';
import { AttendeeEventsScreen, Identity } from '../screens';
import OrganizerEventsNavigation from './OrganizerNavigation';

const OrganizationTopTabNavigator = createMaterialTopTabNavigator();

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
const styles = StyleSheet.create({
  navigator: {
    ...Platform.select({
      web: {
        width: '100vw',
      },
      default: {},
    }),
  },
});

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
      style={styles.navigator}
      initialRouteName={STRINGS.organization_navigation_tab_events}
      screenOptions={{
        swipeEnabled: false,
      }}>
      {screens.map(({ id, title, Component, Badge, Icon }) => (
        <OrganizationTopTabNavigator.Screen
          key={id}
          name={id}
          component={Component}
          options={{
            title: title || id,
            tabBarBadge: Badge,
            tabBarIcon: Icon,
          }}
        />
      ))}
    </OrganizationTopTabNavigator.Navigator>
  );
};

export default LaoNavigation;
