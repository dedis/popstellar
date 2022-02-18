import React from 'react';
import { StyleSheet, View } from 'react-native';

import STRINGS from 'res/strings';
import { CameraButton, RecorderButton, TextBlock } from 'core/components';

/**
 * The witness video component: camera button and a record button
 *
 * TODO store photo and video to the device
 * TODO later share the video to everyone
 */

const styles = StyleSheet.create({
  flexBoxContainer: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
});

const WitnessCamera = () => (
  <>
    <TextBlock text={STRINGS.connect_scanning_camera_view} />
    <View style={styles.flexBoxContainer}>
      <CameraButton action={() => console.log('Camera button pressed')} />
      <RecorderButton action={() => console.log('Recording button pressed')} />
    </View>
  </>
);

export default WitnessCamera;
