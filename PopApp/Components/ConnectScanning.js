import React from 'react';
import { StyleSheet, View, Text } from 'react-native';
import PropTypes from 'prop-types';

import STRINGS from '../res/strings';
import { Typography } from '../Styles';
import CameraButton from './CameraButton';

/**
* Scanning connect component
*
* In the future will scan a QR code and connect to the LAO, not just a dummy button
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-evenly',
  },
  text: {
    ...Typography.base,
  },
});

const ConnectScanning = ({ navigation }) => (
  <View style={styles.container}>
    <Text style={styles.text}>{STRINGS.connect_scanning_camera_view}</Text>
    <CameraButton action={() => { navigation.navigate('Connecting'); }} />
  </View>
);

ConnectScanning.propTypes = {
  navigation: PropTypes.shape({
    navigate: PropTypes.func.isRequired,
  }).isRequired,
};

export default ConnectScanning;
