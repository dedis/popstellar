import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import STRINGS from 'res/strings';
import SocialHome from './SocialHome';
import SocialUserProfile from './SocialUserProfile';

/**
 * Defines the Social media home stack navigation. It can go from the home screen to the profile
 * of a given user.
 */

const Stack = createStackNavigator();

export default function SocialMediaHomeNavigation() {
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen
        name={STRINGS.organizer_navigation_tab_home}
        component={SocialHome}
      />
      <Stack.Screen
        name={STRINGS.organizer_navigation_tab_create_event}
        component={SocialUserProfile}
      />
    </Stack.Navigator>
  );
}
