import React from 'react';
import {
  Platform, StyleSheet,
} from 'react-native';
import { createMaterialTopTabNavigator } from '@react-navigation/material-top-tabs';
import { connect } from 'react-redux';

import STRINGS from '../res/strings';

import Attendee from '../Components/Attendee';
import Identity from '../Components/Identity';
import MytabBar from '../Components/OrganizerMaterialTab';
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

function OrganizationNavigation() {
  return (
    <OrganizationTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.organization_navigation_tab_attendee}
      // eslint-disable-next-line react/jsx-props-no-spreading
      tabBar={(props) => <MytabBar {...props} />}
    >
      <OrganizationTopTabNavigator.Screen
        name={STRINGS.organization_navigation_tab_attendee}
        component={Attendee}
      />
      <OrganizationTopTabNavigator.Screen
        name={STRINGS.organization_navigation_tab_organizer}
        component={OrganizerNavigation}
      />
      <OrganizationTopTabNavigator.Screen
        name={STRINGS.organization_navigation_tab_witness}
        component={WitnessNavigation}
      />
      <OrganizationTopTabNavigator.Screen
        name={STRINGS.organization_navigation_tab_identity}
        component={Identity}
      />
    </OrganizationTopTabNavigator.Navigator>
  );
}

const mapStateToProps = (state) => (
  {
    LAO_ID: state.LAO_ID,
  }
);

export default connect(mapStateToProps)(OrganizationNavigation);
