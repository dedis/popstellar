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
* create a tab navigator between the Home, Attendee, Organizer, Witness and Identity component
*
* the SafeAreaView resolves problem with status bar overlap
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
        name="Organizer"
        component={OrganizerNavigation}
      />
      <OrganizationTopTabNavigator.Screen
        name="Witness"
        component={WitnessNavigation}
      />
      <OrganizationTopTabNavigator.Screen
        name={STRINGS.organization_navigation_tab_identity}
        component={Identity}
      />
    </OrganizationTopTabNavigator.Navigator>
  );
}

const mapStateToProps = (state) => ({
    LAO_ID: state.toggleAppNavigationScreenReducer.LAO_ID,
});

export default connect(mapStateToProps)(OrganizationNavigation);
