import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity, TextStyle, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';
import { useNavigation } from '@react-navigation/native';
import { connect } from 'react-redux';
import { Dispatch } from 'redux';

import { connectToLao as connectToLaoAction } from 'store';
import { Lao } from 'model/objects';

import STRINGS from 'res/strings';
import { Spacing, Typography } from 'styles';

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

const LAOItem = ({ LAO, connectToLao }: IPropTypes) => {
  const navigation = useNavigation();

  const handlePress = () => {
    connectToLao(LAO.toState());
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

const propTypes = {
  LAO: PropTypes.instanceOf(Lao).isRequired,
  connectToLao: PropTypes.func.isRequired,
};
LAOItem.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const mapDispatchToProps = (dispatch: Dispatch) => ({
  connectToLao: (lao: Lao) => dispatch(connectToLaoAction(lao.toState())),
});

export default connect(null, mapDispatchToProps)(LAOItem);
