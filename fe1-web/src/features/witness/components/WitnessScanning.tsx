import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import STRINGS from 'res/strings';
import { Typography } from 'styles';
import PROPS_TYPE from 'res/Props';
import { CameraButton } from 'core/components';

/**
 * Scanning witness component: a camera view and a camera button
 *
 * The camera button do a goBack
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
    <CameraButton
      action={() => {
        navigation.goBack();
      }}
    />
  </View>
);

WitnessScanning.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default WitnessScanning;
