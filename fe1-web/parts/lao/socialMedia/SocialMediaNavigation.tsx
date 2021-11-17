import * as React from 'react';
import { Ionicons } from '@expo/vector-icons';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import STRINGS from 'res/strings';
import { red, gray } from 'styles/colors';

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

        switch (route.name) {
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
            console.log('wrong route.');
            break;
        }

        return <Ionicons name={iconName} size={23} color={color} />;
      },
      tabBarActiveTintColor: red,
      tabBarInactiveTintColor: gray,
    })}
  >
    <Tab.Screen name={STRINGS.social_media_navigation_tab_home} component={SocialHome} />
    <Tab.Screen name={STRINGS.social_media_navigation_tab_search} component={SocialSearch} />
    <Tab.Screen name={STRINGS.social_media_navigation_tab_follows} component={SocialFollows} />
    <Tab.Screen name={STRINGS.social_media_navigation_tab_profile} component={SocialProfile} />
  </Tab.Navigator>
);

export default SocialMediaNavigation;
