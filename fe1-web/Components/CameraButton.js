import React from 'react';
import {
  StyleSheet, Image, View, TouchableOpacity,
} from 'react-native';
import PropTypes from 'prop-types';

import { Colors } from '../Styles';

/**
 * Camera button component: a design button
 *
 * Show a camera button that apply the function action
 * when it is press.
 *
 * use action parameter to define the onPress action
*/

const cameraIc = require('../res/img/ic_camera.png');

const styles = StyleSheet.create({
  icon: {
    width: 64,
    height: 64,
  },
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 80,
    height: 80,
    backgroundColor: Colors.blue,
    borderRadius: 80,
  },
});

function CameraButton({ action }) {
  return (
    <View>
      <TouchableOpacity style={styles.button} onPress={() => { action(); }}>
        <Image style={styles.icon} source={cameraIc} />
      </TouchableOpacity>
    </View>
  );
}

CameraButton.propTypes = {
  action: PropTypes.func.isRequired,
};

export default CameraButton;
