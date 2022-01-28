import React, { useState } from 'react';
import {
  StyleSheet, View, TouchableOpacity,
} from 'react-native';
import PropTypes from 'prop-types';

import { Colors } from 'styles';
import circularButtonStyles from 'styles/stylesheets/circlarButtonStyles';

/**
 * Recorder button that executes an onPress action given in props
 *
 * Displays a different icon depending if the witness is currently recording or not
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
});

function RecorderButton({ action }: IPropTypes) {
  const [isRecording, setIsRecording] = useState(false);

  return (
    <TouchableOpacity
      style={[circularButtonStyles.button, { backgroundColor: Colors.red }]}
      onPress={() => {
        action();
        setIsRecording(!isRecording);
      }}
    >
      <View style={[styles.center, { borderRadius: isRecording ? 5 : 30 }]} />
    </TouchableOpacity>
  );
}

const propTypes = {
  action: PropTypes.func.isRequired,
};
RecorderButton.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RecorderButton;
