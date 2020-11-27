import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import STRINGS from '../res/strings';
import { Typography } from '../Styles';
import CameraButton from './CameraButton';
import PROPS_TYPE from '../res/Props';

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

const ConnectScanning = ({ navigation }) => {
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
