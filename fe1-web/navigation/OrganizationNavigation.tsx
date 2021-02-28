import React from 'react';
import {
  Platform, StyleSheet,
} from 'react-native';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import { connect } from 'react-redux';

import STRINGS from 'res/strings';

import { Lao } from 'model/objects';

import Attendee from 'parts/lao/attendee/Attendee';
import Identity from 'parts/lao/Identity';
import MyTabBar from 'components/OrganizerMaterialTab';

import PropTypes from 'prop-types';
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

// eslint-disable-next-line @typescript-eslint/no-unused-vars
function OrganizationNavigation(props: IPropTypes) {
  const isOrganizer = true; // TODO get isOrganizer directly
  const isWitness = false; // TODO get isWitness directly

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
  lao: PropTypes.instanceOf(Lao),
};

OrganizationNavigation.defaultProps = {
  lao: undefined,
};

const mapStateToProps = (state: any) => ({
  lao: Lao.fromState(state.openedLao),
});

export default connect(mapStateToProps)(OrganizationNavigation);
