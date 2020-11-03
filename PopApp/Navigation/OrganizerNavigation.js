import React from 'react';
import {
  Platform, StyleSheet, View, Text, TouchableOpacity,
} from 'react-native';
import { createMaterialTopTabNavigator, MaterialTopTabBar } from '@react-navigation/material-top-tabs';
import PropTypes from 'prop-types';
import Color from 'color';

import { useTheme } from '@react-navigation/native';
import STRINGS from '../res/strings';

import Attendee from '../Components/Attendee';
import Identity from '../Components/Identity';

const OrganizerTopTabNavigator = createMaterialTopTabNavigator();

/**
* The organizer tab navigation component
*
* create a tab navigator between the Home, Attendee, Organizer, Witness and Identity component
*
* the SafeAreaView resolves problem with status bar overlap
*/
const styles = StyleSheet.create({
  view: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignContent: 'center',
    shadowColor: '#000000',
    shadowOpacity: 0.8,
    shadowRadius: StyleSheet.hairlineWidth,
    shadowOffset: {
      height: StyleSheet.hairlineWidth,
      width: 0,
    },
    elevation: 2,
  },
  text: {
    justifyContent: 'center',
    alignContent: 'center',
    textAlign: 'center',
    textAlignVertical: 'center',
    backgroundColor: 'transparent',
    textTransform: 'uppercase',
  },

  navigator: {
    ...Platform.select({
      web: {
        width: '100vw',
      },
      default: {},
    }),
  },
});

const MytabBar = (props) => {
  const { colors } = useTheme();
  const inactiveColor = Color(colors.text).alpha(0.5).rgb().string();
  const LAO = { name: 'test' }; // props.state.routes[0].params.LAOItem;

  return (
    <View style={[styles.view, { backgroundColor: colors.card }]}>
      <TouchableOpacity style={{ flex: 1 }} onPress={() => props.navigation.navigate('Home')}>
        <Text style={[{ flex: 1, color: inactiveColor }, styles.text]}>Home</Text>
      </TouchableOpacity>
      <MaterialTopTabBar
        {...props}
        style={{ flex: props.navigationState.routes.length, elevation: 0 }}
      />
      <Text style={[{ flex: 1 }, styles.text]}>{LAO.name}</Text>
      {console.log(props)}
      {console.log(LAO)}
    </View>
  );
};

MytabBar.propTypes = {
  navigation: PropTypes.shape({
    navigate: PropTypes.func.isRequired,
  }).isRequired,
  navigationState: PropTypes.shape({
    routes: PropTypes.arrayOf.isRequired,
  }).isRequired,
};

export default function OrganizerNavigation() {
  // const LAO = props.navigationState.routes[0].params;

  return (
    <OrganizerTopTabNavigator.Navigator
      style={styles.navigator}
      initialRouteName={STRINGS.organizer_navigation_tab_attendee}
      tabBar={(props) => <MytabBar {...props} />}
    >
      <OrganizerTopTabNavigator.Screen
        name={STRINGS.organizer_navigation_tab_attendee}
        component={Attendee}
      />
      <OrganizerTopTabNavigator.Screen
        name={STRINGS.organizer_navigation_tab_identity}
        component={Identity}
      />
    </OrganizerTopTabNavigator.Navigator>
  );
}
