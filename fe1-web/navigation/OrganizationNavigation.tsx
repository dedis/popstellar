import React from 'react';
import {
  Platform, StyleSheet,
} from 'react-native';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import { connect } from 'react-redux';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';

import { KeyPairStore } from 'store';
import { Lao } from 'model/objects';

import Attendee from 'components/Attendee';
import Identity from 'components/Identity';
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

interface IPropTypes {
  lao: Lao;
}

function OrganizationNavigation(props: IPropTypes) {
  const { lao } = props;
  const pubKey = KeyPairStore.get().publicKey;
  const isOrganizer = lao.organizer ? lao.organizer === pubKey : false;
  const isWitness = lao.witnesses ? lao.witnesses.includes(pubKey) : false;

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
}

OrganizationNavigation.propTypes = {
  lao: PROPS_TYPE.LAO,
};

OrganizationNavigation.defaultProps = {
  lao: undefined,
};

const mapStateToProps = (state) => ({
  lao: state.openedLao.lao,
});

export default connect(mapStateToProps)(OrganizationNavigation);
