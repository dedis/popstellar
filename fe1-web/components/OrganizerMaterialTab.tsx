import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity,
} from 'react-native';
import { MaterialTopTabBar } from '@react-navigation/material-top-tabs';
import Color from 'color';
import { useSelector } from 'react-redux';

import { useNavigation, useTheme } from '@react-navigation/native';
import STRINGS from 'res/strings';
import { ActionOpenedLaoReducer, makeCurrentLao } from 'store';

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

const MyTabBar = (props) => {
  const { colors } = useTheme();
  const inactiveColor = Color(colors.text).alpha(0.5).rgb().string();

  // NOTE : this file is being refactored!
  const navigation = useNavigation();

  const laoSelect = makeCurrentLao();
  const lao = useSelector(laoSelect);

  const { dispatch, navigationState } = props;
  const nbRoutes = navigationState.routes.length;

  const homePress = () => {
    const action = { type: ActionOpenedLaoReducer.SET_OPENED_LAO, value: {} };
    dispatch(action);
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

export default MyTabBar;
