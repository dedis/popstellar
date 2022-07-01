import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { Color } from '../styles';
import circularButtonStyles from '../styles/stylesheets/circularButtonStyles';
import PoPTouchableOpacity from './PoPTouchableOpacity';

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
    backgroundColor: Color.white,
    borderRadius: 30,
  } as ViewStyle,
  iconRecording: {
    borderRadius: 5,
  } as ViewStyle,
  iconNotRecording: {
    borderRadius: 30,
  } as ViewStyle,
});

function RecorderButton({ action }: IPropTypes) {
  const [isRecording, setIsRecording] = useState(false);

  return (
    <PoPTouchableOpacity
      style={[circularButtonStyles.button, { backgroundColor: Color.red }]}
      onPress={() => {
        action();
        setIsRecording(!isRecording);
      }}>
      <View
        style={
          isRecording
            ? [styles.center, styles.iconRecording]
            : [styles.center, styles.iconNotRecording]
        }
      />
    </PoPTouchableOpacity>
  );
}

const propTypes = {
  action: PropTypes.func.isRequired,
};
RecorderButton.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default RecorderButton;
