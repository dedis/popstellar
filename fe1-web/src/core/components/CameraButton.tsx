import React from 'react';
import { StyleSheet, Image, View, TouchableOpacity, ImageStyle } from 'react-native';
import PropTypes from 'prop-types';

import circularButtonStyles from 'core/styles/stylesheets/circularButtonStyles';

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
      <TouchableOpacity style={circularButtonStyles.button} onPress={action}>
        <Image style={styles.icon} source={cameraImage} />
      </TouchableOpacity>
    </View>
  );
}

const propTypes = {
  action: PropTypes.func.isRequired,
};
CameraButton.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CameraButton;
