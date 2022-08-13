import { Ionicons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import React, { useState } from 'react';
import { useSelector } from 'react-redux';

import { SocialParamList } from 'core/navigation/typing/SocialParamList';
import { PublicKey } from 'core/objects';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { SocialHooks } from '../hooks';
import { SocialFeature } from '../interface';
import { SocialFollows, SocialHome, SocialProfile } from '../screens';
import SocialSearchNavigation from './SocialSearchNavigation';

const Tab = createBottomTabNavigator<SocialParamList>();

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

  const lao = useSelector(SocialHooks.useCurrentLao);

  if (lao === undefined) {
    throw new Error('LAO is currently undefined, impossible to access to Social Media');
  }

  // Get the pop token of the user using the last tokenized roll call
  const rollCallId = lao.last_tokenized_roll_call_id;
  if (rollCallId === undefined) {
    throw new Error(
      'Last tokenized roll call id is undefined, impossible to access to Social Media',
    );
  }
  const rollCall: SocialFeature.RollCall | undefined = SocialHooks.useRollCallById(rollCallId);

  SocialHooks.useSocialContext()
    .generateToken(lao.id, rollCallId)
    .then((token) => {
      if (rollCall?.containsToken(token)) {
        setCurrentUserPublicKey(token.publicKey);
      }
    })
    // If an error happens when generating the token, it should not affect the Social Media
    .catch(() => {
      /* noop */
    });

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: iconSelector(route.name),

        tabBarActiveTintColor: Color.accent,
        tabBarInactiveTintColor: Color.inactive,
        headerLeftContainerStyle: {
          paddingLeft: Spacing.contentSpacing,
        },
        headerRightContainerStyle: {
          paddingRight: Spacing.contentSpacing,
        },
        headerTitleStyle: Typography.topNavigationHeading,
        headerTitleAlign: 'center',
      })}>
      <Tab.Screen
        name={STRINGS.social_media_navigation_tab_home}
        component={SocialHome}
        initialParams={{
          currentUserPublicKey,
        }}
      />
      <Tab.Screen name={STRINGS.social_media_navigation_tab_search}>
        {() => <SocialSearchNavigation currentUserPublicKey={currentUserPublicKey} />}
      </Tab.Screen>
      <Tab.Screen
        name={STRINGS.social_media_navigation_tab_follows}
        component={SocialFollows}
        initialParams={{
          currentUserPublicKey,
        }}
      />
      <Tab.Screen
        name={STRINGS.social_media_navigation_tab_profile}
        component={SocialProfile}
        initialParams={{
          currentUserPublicKey,
        }}
      />
    </Tab.Navigator>
  );
};

export default SocialMediaNavigation;
