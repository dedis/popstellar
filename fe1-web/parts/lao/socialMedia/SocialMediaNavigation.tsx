import * as React from 'react';
import { Ionicons } from '@expo/vector-icons';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import STRINGS from 'res/strings';

import SocialHome from './SocialHome';
import SocialFollows from './SocialFollows';
import SocialSearch from './SocialSearch';
import SocialProfile from './SocialProfile';

const Tab = createMaterialTopTabNavigator();

const SocialMediaNavigation = () => (
  <Tab.Navigator
    screenOptions={({ route }) => ({
      tabBarIcon: ({ color }) => {
        let iconName;

        if (route.name === STRINGS.social_media_navigation_tab_home) {
          iconName = 'home';
        } else if (route.name === STRINGS.social_media_navigation_tab_search) {
          iconName = 'search';
        } else if (route.name === STRINGS.social_media_navigation_tab_follows) {
          iconName = 'people';
        } else if (route.name === STRINGS.social_media_navigation_tab_profile) {
          iconName = 'person';
        }

        return <Ionicons name={iconName} size={23} color={color} />;
      },
      tabBarActiveTintColor: 'tomato',
      tabBarInactiveTintColor: 'gray',
    })}
  >
    <Tab.Screen name={STRINGS.social_media_navigation_tab_home} component={SocialHome} />
    <Tab.Screen name={STRINGS.social_media_navigation_tab_search} component={SocialSearch} />
    <Tab.Screen name={STRINGS.social_media_navigation_tab_follows} component={SocialFollows} />
    <Tab.Screen name={STRINGS.social_media_navigation_tab_profile} component={SocialProfile} />
  </Tab.Navigator>
);

export default SocialMediaNavigation;
