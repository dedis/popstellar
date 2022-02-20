import React from 'react';
import { StyleSheet, View, Text, TouchableOpacity, TextStyle, ViewStyle } from 'react-native';
import PropTypes from 'prop-types';
import { useNavigation } from '@react-navigation/native';

import { Spacing, Typography } from 'core/styles';
import STRINGS from 'resources/strings';

import { Lao } from '../objects';

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

const LaoItem = ({ LAO }: IPropTypes) => {
  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

  const handlePress = () => {
    navigation.navigate(STRINGS.connect_confirm_title, {
      laoIdIn: LAO.id.valueOf(),
      url: LAO.server_address,
    });
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
};
LaoItem.propTypes = propTypes;
type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default LaoItem;
