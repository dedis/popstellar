import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import CameraButton from './CameraButton';
import STRINGS from '../res/strings';
import { Typography } from '../Styles';
import RecorderButton from './RecorderButton';

/**
 * The witness video component: camer view, camera button and a record button
 *
 * The buttons do nothing
 *
 * TODO store photo and video to the device
 * TODO later share the video to everyone
*/

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-evenly',
  },
  buttonContainer: {
    flexDirection: 'row',
  },
  text: {
    ...Typography.base,
  },
});

const WitnessVideo = () => (
  <View style={styles.container}>
    <Text style={styles.text}>{STRINGS.connect_scanning_camera_view}</Text>
    <View style={styles.buttonContainer}>
      <CameraButton action={() => {}} />
      <RecorderButton action={() => {}} />
    </View>
  </View>
);

export default WitnessVideo;
