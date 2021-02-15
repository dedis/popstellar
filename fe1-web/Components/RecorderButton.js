import React, { useState } from 'react';
import {
  StyleSheet, View, TouchableOpacity,
} from 'react-native';
import PropTypes from 'prop-types';

import { Colors } from '../styles';

/**
 * Recorder button component: a design button
 *
 * Show a recorder button that apply the function action
 * when it is press.
 *
 * use action parameter to define the onPress action
*/

const styles = StyleSheet.create({
  center: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 30,
    height: 30,
    backgroundColor: Colors.white,
    borderRadius: 30,
  },
  button: {
    justifyContent: 'center',
    alignItems: 'center',
    width: 80,
    height: 80,
    backgroundColor: Colors.red,
    borderRadius: 80,
  },
});

function RecorderButton({ action }) {
  const [isRecording, toggleRecording] = useState(false);

  return (
    <View>
      <TouchableOpacity
        style={styles.button}
        onPress={() => { action(); toggleRecording(!isRecording); }}
      >
        <View style={[styles.center, { borderRadius: isRecording ? 5 : 30 }]} />
      </TouchableOpacity>
    </View>
  );
}

RecorderButton.propTypes = {
  action: PropTypes.func.isRequired,
};

export default RecorderButton;
