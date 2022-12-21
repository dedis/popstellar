import { createStackNavigator } from '@react-navigation/stack';
import React from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { stackScreenOptionsWithHeader } from 'core/navigation/ScreenOptions';
import { SocialHomeParamList } from 'core/navigation/typing/SocialHomeParamList';
import STRINGS from 'resources/strings';

import { SocialHome, SocialNewChirp } from '../screens';
import { SocialHomeTopRight } from '../screens/SocialHome';

/**
 * Defines the social media search navigation. It goes from the list of attendees to the profile
 * of them.
 */

const Stack = createStackNavigator<SocialHomeParamList>();

const SocialHomeNavigation = () => {
  return (
    <Stack.Navigator screenOptions={stackScreenOptionsWithHeader}>
      <Stack.Screen
        name={STRINGS.social_media_home_navigation_home}
        component={SocialHome}
        options={{
          title: STRINGS.social_media_home_navigation_home_title,
          headerLeft: DrawerMenuButton,
          headerRight: SocialHomeTopRight,
        }}
      />
      <Stack.Screen
        name={STRINGS.social_media_home_navigation_new_chirp}
        component={SocialNewChirp}
      />
    </Stack.Navigator>
  );
};

export default SocialHomeNavigation;
