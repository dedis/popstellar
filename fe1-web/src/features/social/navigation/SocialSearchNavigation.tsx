import { createStackNavigator } from '@react-navigation/stack';
import PropTypes from 'prop-types';
import React from 'react';

import { SocialSearchParamList } from 'core/navigation/typing/SocialSearchParamList';
import { PublicKey } from 'core/objects';
import STRINGS from 'resources/strings';

import { SocialSearch, SocialUserProfile } from '../screens';

/**
 * Defines the social media search navigation. It goes from the list of attendees to the profile
 * of them.
 */

const Stack = createStackNavigator<SocialSearchParamList>();

const SocialSearchNavigation = (props: IPropTypes) => {
  const { currentUserPublicKey } = props;
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}>
      <Stack.Screen
        name={STRINGS.social_media_navigation_tab_attendee_list}
        component={SocialSearch}
        initialParams={{
          currentUserPublicKey,
        }}
      />
      <Stack.Screen
        name={STRINGS.social_media_navigation_tab_user_profile}
        component={SocialUserProfile}
        initialParams={{
          currentUserPublicKey,
          userPublicKey: currentUserPublicKey,
        }}
      />
    </Stack.Navigator>
  );
};

const propTypes = {
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialSearchNavigation.prototype = propTypes;

type IPropTypes = {
  currentUserPublicKey: PublicKey;
};

export default SocialSearchNavigation;
