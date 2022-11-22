import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { LaoEventsParamList } from 'core/navigation/typing/LaoEventsParamList';
import STRINGS from 'resources/strings';

import { LaoHooks } from '../hooks';
import EventsScreen, { EventsScreenHeader, EventsScreenHeaderRight } from '../screens/EventsScreen';

/**
 * Define the Organizer stack navigation
 * four different screen (OrganizerScreen, CreateEvent, RollCallScanning)
 *
 * The app are not use in the stack order, only organizer to one of the other screen
 */

const Stack = createStackNavigator<LaoEventsParamList>();

export default function EventsNavigation() {
  const screens = LaoHooks.useEventsNavigationScreens();

  return (
    <Stack.Navigator
      initialRouteName={STRINGS.navigation_lao_events_home}
      screenOptions={stackScreenOptionsWithHeader}>
      <Stack.Screen
        name={STRINGS.navigation_lao_events_home}
        component={EventsScreen}
        options={{
          title: STRINGS.navigation_lao_events_home_title,
          headerTitle: EventsScreenHeader,
          headerLeft: DrawerMenuButton,
          headerRight: EventsScreenHeaderRight,
        }}
      />
      {screens.map(
        ({ id, title, headerTitle, headerLeft, headerRight, headerShown, Component }) => (
          <Stack.Screen
            name={id}
            key={id}
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
    </Stack.Navigator>
  );
}
