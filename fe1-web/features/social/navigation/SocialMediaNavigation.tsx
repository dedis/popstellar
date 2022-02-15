import * as React from 'react';
import { Ionicons } from '@expo/vector-icons';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import STRINGS from 'res/strings';
import { gray, popBlue } from 'styles/colors';
import { useEffect, useState } from 'react';
import { PublicKey, RollCall } from 'model/objects';
import { generateToken } from 'model/objects/wallet';
import { makeCurrentLao, makeEventGetter } from 'store';
import { useSelector } from 'react-redux';
import SocialProfile from 'features/social/screens/SocialProfile';
import SocialFollows from 'features/social/screens/SocialFollows';
import SocialHome from 'features/social/screens/SocialHome';
import SocialSearchNavigation from 'features/social/navigation/SocialSearchNavigation';

/**
 * This class manages the social media navigation and creates the corresponding navigation bar.
 */

const Tab = createMaterialTopTabNavigator();

const SocialMediaNavigation = () => {
  const [currentUserPublicKey, setCurrentUserPublicKey] = useState(new PublicKey(''));

  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  if (lao === undefined) {
    throw new Error('LAO is currently undefined, impossible to access to Social Media');
  }

  // Get the pop token of the user using the last tokenized roll call
  const rollCallId = lao.last_tokenized_roll_call_id;
  const eventSelect = makeEventGetter(lao.id, rollCallId);
  const rollCall: RollCall = useSelector(eventSelect) as RollCall;

  // This will be run again each time the lao.last_tokenized_roll_call_id changes
  useEffect(() => {
    generateToken(lao.id, rollCallId).then((token) => {
      if (token && rollCall.containsToken(token)) {
        setCurrentUserPublicKey(token.publicKey);
      }
    });
  }, [lao.last_tokenized_roll_call_id]);

  return (
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
              console.error('wrong route.');
              break;
          }

          return <Ionicons name={iconName} size={23} color={color} />;
        },
        tabBarActiveTintColor: popBlue,
        tabBarInactiveTintColor: gray,
        swipeEnabled: false,
      })}
    >
      <Tab.Screen name={STRINGS.social_media_navigation_tab_home}>
        {() => <SocialHome currentUserPublicKey={currentUserPublicKey} />}
      </Tab.Screen>
      <Tab.Screen name={STRINGS.social_media_navigation_tab_search}>
        {() => <SocialSearchNavigation currentUserPublicKey={currentUserPublicKey} />}
      </Tab.Screen>
      <Tab.Screen
        name={STRINGS.social_media_navigation_tab_follows}
        component={SocialFollows}
      />
      <Tab.Screen name={STRINGS.social_media_navigation_tab_profile}>
        {() => <SocialProfile currentUserPublicKey={currentUserPublicKey} />}
      </Tab.Screen>
    </Tab.Navigator>
  );
};

export default SocialMediaNavigation;
