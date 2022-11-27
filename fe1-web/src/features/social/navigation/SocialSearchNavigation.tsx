import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import { stackScreenOptionsWithoutHeader } from 'core/navigation/ScreenOptions';
import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import STRINGS from 'resources/strings';

import { SocialSearch, SocialUserProfile } from '../screens';

/**
 * Defines the social media search navigation. It goes from the list of attendees to the profile
 * of them.
 */

const Stack = createStackNavigator<SocialSearchParamList>();

const SocialSearchNavigation = () => {
  return (
    <Stack.Navigator screenOptions={stackScreenOptionsWithoutHeader}>
      <Stack.Screen
        name={STRINGS.social_media_navigation_tab_attendee_list}
        component={SocialSearch}
      />
      <Stack.Screen
        name={STRINGS.social_media_navigation_tab_user_profile}
        component={SocialUserProfile}
      />
    </Stack.Navigator>
  );
};

export default SocialSearchNavigation;
