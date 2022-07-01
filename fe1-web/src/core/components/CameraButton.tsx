import PropTypes from 'prop-types';
import React from 'react';
import { Image, ImageStyle, StyleSheet, View } from 'react-native';

import circularButtonStyles from '../styles/stylesheets/circularButtonStyles';
import PoPTouchableOpacity from './PoPTouchableOpacity';

const cameraImage = require('resources/img/ic_camera.png');

/**
 * Camera button that executes an onPress action given in props
 */

const styles = StyleSheet.create({
  icon: {
    width: 64,
    height: 64,
  } as ImageStyle,
});

function CameraButton({ action }: IPropTypes) {
  return (
    <View>
      <PoPTouchableOpacity style={circularButtonStyles.button} onPress={action}>
        <Image style={styles.icon} source={cameraImage} />
      </PoPTouchableOpacity>
    </View>
  );
}

const propTypes = {
  action: PropTypes.func.isRequired,
};
CameraButton.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CameraButton;
