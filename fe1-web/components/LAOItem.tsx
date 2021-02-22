import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity, TextStyle, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';
import { useNavigation } from '@react-navigation/native';
import { connect } from 'react-redux';
import { AnyAction } from 'redux';

import STRINGS from 'res/strings';
import { Spacing, Typography } from 'styles';
import PROPS_TYPE from 'res/Props';
import { ActionOpenedLaoReducer } from 'store/Actions';
import { Lao } from 'model/objects';

/**
  * The LAO item component: name of LAO
  *
  * On click go to the organization screen and store the ID of the LAO for the organization screen
*/
const styles = StyleSheet.create({
  view: {
    marginBottom: Spacing.xs,
  } as ViewStyle,
  text: {
    ...Typography.base,
    borderWidth: 1,
    borderRadius: 5,
  } as TextStyle,
});

interface IPropTypes {
  LAO: Lao;
  dispatch: (arg: AnyAction) => any;
}

const LAOItem = ({ LAO, dispatch }: IPropTypes) => {
  const navigation = useNavigation();

  const handlePress = () => {
    const action2 = { type: ActionOpenedLaoReducer.SET_OPENED_LAO, value: LAO };
    dispatch(action2);
    navigation.navigate(STRINGS.app_navigation_tab_organizer);
  };

  return (
    <View style={styles.view}>
      <TouchableOpacity onPress={() => handlePress()}>
        <Text style={styles.text}>{LAO.name}</Text>
      </TouchableOpacity>
    </View>
  );
};

LAOItem.propTypes = {
  LAO: PROPS_TYPE.LAO.isRequired,
  dispatch: PropTypes.func.isRequired,
};

export default connect()(LAOItem);
