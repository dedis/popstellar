import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import STRINGS from 'res/strings';
import { Typography } from '../styles';
import CameraButton from './CameraButton';
import PROPS_TYPE from '../res/Props';

/**
 * Scanning witness component: a camera view and a camera button
 *
 * The cammera button do a goBack
 *
 * TODO press on the button will scan a QR code and add a witness
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

const WitnessScanning = ({ navigation }) => (
  <View style={styles.container}>
    <Text style={styles.text}>{STRINGS.witness_scan}</Text>
    <CameraButton action={() => { navigation.goBack(); }} />
  </View>
);

WitnessScanning.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default WitnessScanning;
