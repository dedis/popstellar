import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import STRINGS from '../res/strings';
import { Typography } from '../Styles';
import CameraButton from './CameraButton';
import PROPS_TYPE from '../res/Props';

/**
 * Scanning connect component: a camera view, and a camera button
 *
 * In the future will scan a QR code and connect to the LAO, not just a dummy button
 *
 * The camera view is just a string, in the future will be the real camera view
 * The camera button redirect to the ConnectConncting component.
 *
 * TODO use the camera to scan a QR code and give the URL find to the ConnectConnecting component
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

const ConnectScanning = ({ navigation }) => {
  /* Remove the user to go back to the ConnectUnapprove as he has already given
     his permission to use the camera */
  React.useEffect(
    () => navigation.addListener('beforeRemove', (e) => {
      e.preventDefault();
    }),
    [navigation],
  );

  return (
    <View style={styles.container}>
      <Text style={styles.text}>{STRINGS.connect_scanning_camera_view}</Text>
      <CameraButton action={() => { navigation.navigate(STRINGS.connect_connecting_title); }} />
    </View>
  );
};

ConnectScanning.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default ConnectScanning;
