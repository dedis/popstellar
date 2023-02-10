import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useMemo, useState } from 'react';

import DrawerMenuButton from 'core/components/DrawerMenuButton';
import { makeIcon } from 'core/components/PoPIcon';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { LaoParamList } from 'core/navigation/typing/LaoParamList';
import { SocialParamList } from 'core/navigation/typing/social/SocialParamList';
import { PublicKey } from 'core/objects';
import { Color, Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { SocialMediaContext } from '../context';
import { SocialHooks } from '../hooks';
import { SocialFeature } from '../interface';
import { SocialHome, SocialProfile, SocialTopChirps } from '../screens';
import SocialSearchNavigation from './SocialSearchNavigation';

const Tab = createBottomTabNavigator<SocialParamList>();

const homeIcon = makeIcon('home');
const searchIcon = makeIcon('userList');
const topChirpsIcon = makeIcon('topItems');
const profileIcon = makeIcon('profile');

type NavigationProps = CompositeScreenProps<
  StackScreenProps<LaoParamList, typeof STRINGS.navigation_social_media>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

/**
 * This class manages the social media navigation and creates the corresponding navigation bar.
 */
const SocialMediaNavigation = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const [currentUserPopTokenPublicKey, setCurrentUserPopTokenPublicKey] = useState(
    undefined as unknown as PublicKey,
  );

  const lao = SocialHooks.useCurrentLao();

  if (lao === undefined) {
    throw new Error('LAO is currently undefined, impossible to access to Social Media');
  }

  // Get the pop token of the user using the last tokenized roll call
  const rollCallId = lao.last_tokenized_roll_call_id;
  const rollCall: SocialFeature.RollCall | undefined = SocialHooks.useRollCallById(rollCallId);

  SocialHooks.useSocialContext()
    .generateToken(lao.id, rollCallId)
    .then((token) => {
      if (rollCall?.containsToken(token)) {
        setCurrentUserPopTokenPublicKey(token.publicKey);
      }
    })
    // If an error happens when generating the token, it should not affect the Social Media
    .catch(() => {
      /* noop */
    });

  // prevents unnecessary re-renders in components using this react context
  // react by default only performs shallow equality checks which means
  // it will be a different object (try ({a: 1} == {a: 1})) and trigger a re-render
  const contextValue = useMemo(
    () => ({
      currentUserPopTokenPublicKey,
    }),
    [currentUserPopTokenPublicKey],
  );

  return (
    <SocialMediaContext.Provider value={contextValue}>
      <Tab.Navigator
        screenOptions={{
          tabBarActiveTintColor: Color.accent,
          tabBarInactiveTintColor: Color.inactive,
          headerLeft: DrawerMenuButton,
          headerLeftContainerStyle: {
            paddingLeft: Spacing.contentSpacing,
          },
          headerRightContainerStyle: {
            paddingRight: Spacing.contentSpacing,
          },
          headerTitleStyle: Typography.topNavigationHeading,
          headerTitleAlign: 'center',
        }}>
        <Tab.Screen
          name={STRINGS.social_media_navigation_tab_home}
          component={SocialHome}
          options={{
            title: STRINGS.social_media_navigation_tab_home_title,
            tabBarIcon: homeIcon,
          }}
        />
        <Tab.Screen
          name={STRINGS.social_media_navigation_tab_top_chirps}
          component={SocialTopChirps}
          options={{
            title: STRINGS.social_media_navigation_tab_top_chirps_title,
            tabBarIcon: topChirpsIcon,
          }}
        />
        <Tab.Screen
          name={STRINGS.social_media_navigation_tab_search}
          component={SocialSearchNavigation}
          options={{
            title: STRINGS.social_media_navigation_tab_search_title,
            headerShown: false,
            tabBarIcon: searchIcon,
          }}
          listeners={{
            tabPress: (e) => {
              // => do nothing
              e.preventDefault();
              navigation.navigate(STRINGS.navigation_app_lao, {
                screen: STRINGS.navigation_social_media,
                params: {
                  screen: STRINGS.social_media_navigation_tab_search,
                  params: {
                    screen: STRINGS.social_media_search_navigation_attendee_list,
                  },
                },
              });
            },
          }}
        />
        <Tab.Screen
          name={STRINGS.social_media_navigation_tab_profile}
          component={SocialProfile}
          options={{
            title: STRINGS.social_media_navigation_tab_profile_title,
            tabBarIcon: profileIcon,
          }}
        />
      </Tab.Navigator>
    </SocialMediaContext.Provider>
  );
};

export default SocialMediaNavigation;

export const SocialMediaScreen: SocialFeature.LaoScreen = {
  id: STRINGS.navigation_social_media,
  Component: SocialMediaNavigation,
  headerShown: false,
  Icon: makeIcon('socialMedia'),
  headerLeft: DrawerMenuButton,
  order: 10000,
};
