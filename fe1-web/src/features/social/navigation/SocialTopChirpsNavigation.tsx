import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { SocialTopChirpsParamList } from 'core/navigation/typing/social';
import STRINGS from 'resources/strings';

import { SocialTopChirps, SocialUserProfile } from '../screens';

/**
 * Defines the social media search navigation. It goes from the list of attendees to the profile
 * of them.
 */

const Stack = createStackNavigator<SocialTopChirpsParamList>();

const SocialTopChirpsNavigation = () => {
  return (
    <Stack.Navigator screenOptions={stackScreenOptionsWithHeader}>
      <Stack.Screen
        name={STRINGS.social_media_top_chirps_navigation_top_chirps}
        component={SocialTopChirps}
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

export default SocialTopChirpsNavigation;
