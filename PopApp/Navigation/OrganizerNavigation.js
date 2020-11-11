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

const OrganizerTopTabNavigator = createMaterialTopTabNavigator();

/**
* The organizer tab navigation component
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

function OrganizerNavigation() {
  // const LAO = props.navigationState.routes[0].params;

  return (
    <OrganizerTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.organizer_navigation_tab_attendee}
      // eslint-disable-next-line react/jsx-props-no-spreading
      tabBar={(props) => <MytabBar {...props} />}
    >
      <OrganizerTopTabNavigator.Screen
        name={STRINGS.organizer_navigation_tab_attendee}
        component={Attendee}
      />
      <OrganizerTopTabNavigator.Screen
        name="Witness"
        component={WitnessNavigation}
      />
      <OrganizerTopTabNavigator.Screen
        name={STRINGS.organizer_navigation_tab_identity}
        component={Identity}
      />
    </OrganizerTopTabNavigator.Navigator>
  );
}

const mapStateToProps = (state) => (
  {
    LAO_ID: state.LAO_ID,
  }
);

export default connect(mapStateToProps)(OrganizerNavigation);
