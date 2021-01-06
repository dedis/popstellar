import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity,
} from 'react-native';
import PropTypes from 'prop-types';
import { useNavigation } from '@react-navigation/native';
import { connect } from 'react-redux';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';
import PROPS_TYPE from '../res/Props';

/**
  * The LAO item component: name of LAO
  *
  * On click go to the organization screen and store the ID of the LAO for the organization screen
*/
const styles = StyleSheet.create({
  view: {
    marginBottom: Spacing.xs,
  },
  text: {
    ...Typography.base,
    borderWidth: 1,
    borderRadius: 5,
  },
});

const LAOItem = ({ LAO, dispatch }) => {
  const navigation = useNavigation();

  const handlePress = () => {
    const action2 = { type: 'SET_CURRENT_LAO', value: LAO };
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
