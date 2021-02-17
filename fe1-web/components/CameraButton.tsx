import React from 'react';
import {
  StyleSheet, Image, View, TouchableOpacity, ImageStyle,
} from 'react-native';
import PropTypes from 'prop-types';

import { Colors } from 'styles';

const cameraIc = require('../res/img/ic_camera.png');

/**
 * Camera button component: a design button
 *
 * Show a camera button that apply the function action
 * when it is press.
 *
 * use action parameter to define the onPress action
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

interface IPropTypes {
  action: () => any;
}

function CameraButton({ action }: IPropTypes) {
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
