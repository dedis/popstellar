import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { SocialProfileParamList } from 'core/navigation/typing/social';
import STRINGS from 'resources/strings';

import { SocialProfile, SocialUserProfile } from '../screens';

/**
 * Defines the social media search navigation. It goes from the list of attendees to the profile
 * of them.
 */

const Stack = createStackNavigator<SocialProfileParamList>();

const SocialProfileNavigation = () => {
  return (
    <Stack.Navigator screenOptions={stackScreenOptionsWithHeader}>
      <Stack.Screen
        name={STRINGS.social_media_profile_navigation_profile}
        component={SocialProfile}
        options={{
          headerLeft: DrawerMenuButton,
        }}
      />
      <Stack.Screen
        name={STRINGS.social_media_navigation_user_profile}
        component={SocialUserProfile}
      />
    </Stack.Navigator>
  );
};

export default SocialProfileNavigation;
