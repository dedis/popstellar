import React from 'react';
import {
  Platform, StyleSheet,
} from 'react-native';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import { useSelector } from 'react-redux';

import STRINGS from 'res/strings';

import { KeyPairStore, makeCurrentLao } from 'store';

import Attendee from 'parts/lao/attendee/Attendee';
import Identity from 'parts/lao/Identity';
import MyTabBar from 'components/OrganizerMaterialTab';

import WitnessNavigation from './WitnessNavigation';
import OrganizerNavigation from './OrganizerNavigation';

const OrganizationTopTabNavigator = createMaterialTopTabNavigator();

/**
 * The organization tab navigation component
 *
 * create a tab navigator between the Home (fake tab), Attendee, Organizer, Witness
 * and Identity component
 *
 * TODO show only tab corresponding to the role of the user
*/
const styles = StyleSheet.create({
  navigator: {
    ...Platform.select({
      web: {
        width: '100vw',
      },
      default: {},
    }),
  },
});

const OrganizationNavigation = () => {
  const currentLao = makeCurrentLao();
  const lao = useSelector(currentLao);

  if (!lao) {
    return (<> </>);
  }

  const pubKey = KeyPairStore.get().publicKey;
  // const isOrganizer = lao.organizer && pubKey.equals(lao.organizer);
  const isOrganizer = false;
  const isWitness = lao.witnesses ? lao.witnesses.includes(pubKey) : false;
  // console.log("is org : ", isOrganizer, ", is Witness : ", isWitness);

  return (
    <OrganizationTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.organization_navigation_tab_attendee}
      // eslint-disable-next-line react/jsx-props-no-spreading
      tabBar={(p) => <MyTabBar {...p} />}
    >
      {!isOrganizer && !isWitness && (
        <OrganizationTopTabNavigator.Screen
          name={STRINGS.organization_navigation_tab_attendee}
          component={Attendee}
        />
      )}
      {isOrganizer && (
        <OrganizationTopTabNavigator.Screen
          name={STRINGS.organization_navigation_tab_organizer}
          component={OrganizerNavigation}
        />
      )}
      {isWitness && (
        <OrganizationTopTabNavigator.Screen
          name={STRINGS.organization_navigation_tab_witness}
          component={WitnessNavigation}
        />
      )}
      <OrganizationTopTabNavigator.Screen
        name={STRINGS.organization_navigation_tab_identity}
        component={Identity}
      />
    </OrganizationTopTabNavigator.Navigator>
  );
};

export default OrganizationNavigation;
