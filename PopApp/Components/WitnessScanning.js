import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import { Typography } from '../Styles';
import CameraButton from './CameraButton';
import PROPS_TYPE from '../res/Props';
import STRINGS from '../res/strings';

/**
* Scanning witness component
*
* In the future will scan a QR code and add a witness, not just a dummy button
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
