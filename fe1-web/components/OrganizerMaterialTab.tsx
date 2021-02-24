import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity,
} from 'react-native';
import { MaterialTopTabBar } from '@react-navigation/material-top-tabs';
import { useTheme } from '@react-navigation/native';
import { useSelector } from 'react-redux';
import PropTypes from 'prop-types';
import Color from 'color';

import { disconnectFromLao, makeCurrentLao } from 'store';
import { getNetworkManager } from 'network';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';

/**
 * Organizer tab bar
 *
 * Add a home tab at left and the name of the LAO at the right
 *
 * The home tab allow the user to go back to the Home UI
 * THe LAO name is non pressable
 *
 * Design is only correct on Android
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
});

const MyTabBar = (props: IPropTypes) => {
  const { colors } = useTheme();
  const inactiveColor = Color(colors.text).alpha(0.5).rgb().string();
  const currentLao = makeCurrentLao();
  const lao = useSelector(currentLao);
  const { navigation, dispatch, navigationState } = props;
  const nbRoutes = navigationState.routes.length;

  const homePress = () => {
    getNetworkManager().disconnectFromAll();
    dispatch(disconnectFromLao());
    navigation.navigate(STRINGS.app_navigation_tab_home);
  };

  return (
    <View style={[styles.view, { backgroundColor: colors.card }]}>
      <TouchableOpacity
        style={{ flex: 1 }}
        onPress={() => homePress()}
      >
        <Text style={[{ flex: 1, color: inactiveColor }, styles.text]}>Home</Text>
      </TouchableOpacity>
      <MaterialTopTabBar
        // eslint-disable-next-line react/jsx-props-no-spreading
        {...props}
        style={{ flex: nbRoutes, elevation: 0 }}
      />
      <Text style={[{ flex: 1 }, styles.text]}>
        {lao !== undefined ? lao.name : STRINGS.unused }
      </Text>
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
  navigationState: PROPS_TYPE.navigationState.isRequired,
  dispatch: PropTypes.func.isRequired,
};
MyTabBar.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default MyTabBar;
