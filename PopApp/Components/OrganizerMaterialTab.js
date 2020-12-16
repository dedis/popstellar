import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity,
} from 'react-native';
import { MaterialTopTabBar } from '@react-navigation/material-top-tabs';
import PropTypes from 'prop-types';
import Color from 'color';
import { connect } from 'react-redux';

import { useTheme } from '@react-navigation/native';
import STRINGS from '../res/strings';
import PROPS_TYPE from '../res/Props';

/**
 * Organizer tab bar
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

const MytabBar = (props) => {
  const { colors } = useTheme();
  const inactiveColor = Color(colors.text).alpha(0.5).rgb().string();
  const { LAOs } = props;
  const LAO = LAOs.find((x) => x.id === props.LAO_ID);
  const { navigation, dispatch, navigationState } = props;
  const nbRoutes = navigationState.routes.length;

  const homePress = () => {
    const action = { type: 'APP_NAVIGATION_OFF' };
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
        {LAO !== undefined ? LAO.name : STRINGS.unused }
      </Text>
    </View>
  );
};

MytabBar.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
  navigationState: PROPS_TYPE.navigationState.isRequired,
  dispatch: PropTypes.func.isRequired,
  LAO_ID: PropTypes.string,
  LAOs: PropTypes.arrayOf(PROPS_TYPE.LAO).isRequired,
};

MytabBar.defaultProps = {
  LAO_ID: '-1',
};

const mapStateToProps = (state) => ({
  LAO_ID: state.toggleAppNavigationScreenReducer.LAO_ID,
  LAOs: state.connectLAOsReducer.LAOs,
});

export default connect(mapStateToProps)(MytabBar);
