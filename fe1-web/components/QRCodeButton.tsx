import React from 'react';
import {
  StyleSheet, Image, View, TouchableOpacity, ImageStyle,
} from 'react-native';
import PropTypes from 'prop-types';

import { Colors } from 'styles';

const cameraImage = require('../res/img/QRCode.png');

/**
 * QR code button that executes an onPress action given in props
 */

const styles = StyleSheet.create({
  icon: {
    width: 64,
    height: 64,
  } as ImageStyle,
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 80,
    height: 80,
    backgroundColor: Colors.blue,
    borderRadius: 80,
  },
});

function QRCodeButton({ action }: IPropTypes) {
  return (
    <View>
      <TouchableOpacity style={styles.button} onPress={action}>
        <Image style={styles.icon} source={cameraImage} />
      </TouchableOpacity>
    </View>
  );
}

const propTypes = {
  action: PropTypes.func.isRequired,
};
QRCodeButton.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default QRCodeButton;
