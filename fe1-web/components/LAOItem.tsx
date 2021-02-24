import React from 'react';
import {
  StyleSheet, View, Text, TouchableOpacity, TextStyle, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';
import { useNavigation } from '@react-navigation/native';
import { connect } from 'react-redux';

import { Hash, Lao } from 'model/objects';
import { connectToLao as connectToLaoAction } from 'store';

import { Spacing, Typography } from 'styles';
import STRINGS from 'res/strings';
import { Dispatch } from 'redux';

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
    connectToLao(LAO.id);
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
  // dispatching actions returned by action creators
  connectToLao: (id: Hash) => dispatch(connectToLaoAction(id)),
});

const LAOItemContainer = connect(null, mapDispatchToProps)(LAOItem);
LAOItemContainer.propTypes = {
  LAO: PropTypes.instanceOf(Lao).isRequired,
};

export default LAOItemContainer;
