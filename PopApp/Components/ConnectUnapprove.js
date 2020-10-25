import React from 'react';
import {
  StyleSheet, View, Text, Button,
} from 'react-native';
import PropTypes from 'prop-types';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';

/**
* The unapproved component
*
* In the future will ask for camera permission
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-evenly',
  },
  text: {
    ...Typography.base,
  },
  button: {
    marginHorizontal: Spacing.xl,
  },
});

const ConnectUnapprove = ({ navigation }) => (
  <View style={styles.container}>
    <Text style={styles.text}>{STRINGS.connect_description}</Text>
    <View style={styles.button}>
      <Button title={STRINGS.connect_button_camera} onPress={() => { navigation.navigate('Scanning'); }} />
    </View>
  </View>
);

ConnectUnapprove.propTypes = {
  navigation: PropTypes.shape({
    navigate: PropTypes.func.isRequired,
  }).isRequired,
};

export default ConnectUnapprove;
