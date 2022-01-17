import React from 'react';
import { createStackNavigator } from '@react-navigation/stack';
import STRINGS from 'res/strings';
import { PublicKey } from 'model/objects';
import PropTypes from 'prop-types';
import SocialSearch from 'parts/lao/socialMedia/SocialSearch';
import SocialUserProfile from 'parts/lao/socialMedia/SocialUserProfile';

/**
 * Defines the social media search navigation. It goes from the list of attendees to the profile
 * of them.
 */

const Stack = createStackNavigator();

const SocialSearchNavigation = (props: IPropTypes) => {
  const { currentUserPublicKey } = props;
  return (
    <Stack.Navigator
      screenOptions={{
        headerShown: false,
      }}
    >
      <Stack.Screen name={STRINGS.social_media_navigation_tab_search}>
        {() => <SocialSearch currentUserPublicKey={currentUserPublicKey} />}
      </Stack.Screen>
      <Stack.Screen
        name={STRINGS.social_media_navigation_tab_user_profile}
        component={SocialUserProfile}
      />
    </Stack.Navigator>
  );
};

const propTypes = {
  currentUserPublicKey: PropTypes.instanceOf(PublicKey).isRequired,
};

SocialSearchNavigation.prototype = propTypes;

type IPropTypes = {
  currentUserPublicKey: PublicKey,
};

export default SocialSearchNavigation;
