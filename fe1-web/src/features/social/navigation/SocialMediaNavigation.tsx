import { Ionicons } from '@expo/vector-icons';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import React, { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';

import { PublicKey } from 'core/objects';
import { gray, popBlue } from 'core/styles/colors';
import { makeEventGetter } from 'features/events/reducer';
import { selectCurrentLao } from 'features/lao/reducer';
import { RollCall } from 'features/rollCall/objects';
import { generateToken } from 'features/wallet/objects';
import STRINGS from 'resources/strings';

import { SocialFollows, SocialHome, SocialProfile } from '../screens';
import SocialSearchNavigation from './SocialSearchNavigation';

const Tab = createMaterialTopTabNavigator();

const iconSelector =
  (routeName: string) =>
  ({ color }: { color: string }) => {
    let iconName: 'home' | 'search' | 'people' | 'person' | 'stop';

    switch (routeName) {
      case STRINGS.social_media_navigation_tab_home:
        iconName = 'home';
        break;
      case STRINGS.social_media_navigation_tab_search:
        iconName = 'search';
        break;
      case STRINGS.social_media_navigation_tab_follows:
        iconName = 'people';
        break;
      case STRINGS.social_media_navigation_tab_profile:
        iconName = 'person';
        break;
      default:
        iconName = 'stop';
        console.error('Icon could not be rendered correctly. Wrong route name.');
        break;
    }

    return <Ionicons name={iconName} size={23} color={color} />;
  };

/**
 * This class manages the social media navigation and creates the corresponding navigation bar.
 */
const SocialMediaNavigation = () => {
  const [currentUserPublicKey, setCurrentUserPublicKey] = useState(new PublicKey(''));

  const lao = useSelector(selectCurrentLao);

  if (lao === undefined) {
    throw new Error('LAO is currently undefined, impossible to access to Social Media');
  }

  // Get the pop token of the user using the last tokenized roll call
  const rollCallId = lao.last_tokenized_roll_call_id;
  const eventSelect = makeEventGetter(lao.id, rollCallId);
  const rollCall: RollCall = useSelector(eventSelect) as RollCall;

  // This will be run again each time the lao.last_tokenized_roll_call_id changes
  useEffect(() => {
    generateToken(lao.id, rollCallId)
      .then((token) => {
        if (rollCall.containsToken(token)) {
          setCurrentUserPublicKey(token.publicKey);
        }
      })
      // If an error happens when generating the token, it should not affect the Social Media
      .catch(() => {
        /* noop */
      });
  }, [lao.id, lao.last_tokenized_roll_call_id, rollCall, rollCallId]);

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: iconSelector(route.name),
        tabBarActiveTintColor: popBlue,
        tabBarInactiveTintColor: gray,
        swipeEnabled: false,
      })}>
      <Tab.Screen name={STRINGS.social_media_navigation_tab_home}>
        {() => <SocialHome currentUserPublicKey={currentUserPublicKey} />}
      </Tab.Screen>
      <Tab.Screen name={STRINGS.social_media_navigation_tab_search}>
        {() => <SocialSearchNavigation currentUserPublicKey={currentUserPublicKey} />}
      </Tab.Screen>
      <Tab.Screen name={STRINGS.social_media_navigation_tab_follows} component={SocialFollows} />
      <Tab.Screen name={STRINGS.social_media_navigation_tab_profile}>
        {() => <SocialProfile currentUserPublicKey={currentUserPublicKey} />}
      </Tab.Screen>
    </Tab.Navigator>
  );
};

export default SocialMediaNavigation;
