import React from 'react';
import {
  Platform, StyleSheet,
} from 'react-native';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import { connect } from 'react-redux';
import PropTypes from 'prop-types';

import STRINGS from '../res/strings';

import Attendee from '../Components/Attendee';
import Identity from '../Components/Identity';
import MytabBar from '../Components/OrganizerMaterialTab';
import WitnessNavigation from './WitnessNavigation';
import OrganizerNavigation from './OrganizerNavigation';
import PROPS_TYPE from '../res/Props';

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

function OrganizationNavigation(props) {
  const { lao, pubKey } = props;
  const isOrganizer = lao.organizer ? lao.organizer === pubKey : false;
  const isWitness = lao.witnesses ? lao.witnesses.includes(pubKey) : false;

  return (
    <OrganizationTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.organization_navigation_tab_attendee}
      // eslint-disable-next-line react/jsx-props-no-spreading
      tabBar={(p) => <MytabBar {...p} />}
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
  lao: PROPS_TYPE.LAO.isRequired,
  pubKey: PropTypes.string.isRequired,
};

const mapStateToProps = (state) => ({
  lao: state.currentLaoReducer.lao,
  pubKey: state.keypairReducer.pubKey,
});

export default connect(mapStateToProps)(OrganizationNavigation);
