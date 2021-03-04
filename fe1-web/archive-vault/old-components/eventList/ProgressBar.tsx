import React from 'react';
import {
  StyleSheet, Text, View,
} from 'react-native';
import PropTypes from 'prop-types';

import { Colors, Spacing } from 'styles';

// Note: used in PollEvent

/**
 * Progress bar with the percent show at the right
 *
 * progress: real number between 0 and 1
 */

const styles = StyleSheet.create({
  progressBarBackground: {
    flex: 1,
    borderWidth: 1,
    borderRadius: 5,
    backgroundColor: Colors.white,
    height: 5,
    marginHorizontal: Spacing.xs,
  },
  progressFront: {
    flex: 1,
    backgroundColor: Colors.blue,
  },
  container: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  text: {
    width: Spacing.l,
    textAlign: 'right',
  },
});

interface IPropTypes {
  progress: number;
}

function ProgressBar({ progress }: IPropTypes) {
  return (
    <View style={styles.container}>
      <View style={styles.progressBarBackground}>
        <View style={[styles.progressFront, { width: `${progress * 100}%` }]} />
      </View>
      <Text style={styles.text}>{`${progress * 100}%`}</Text>
    </View>
  );
}

ProgressBar.propTypes = {
  progress: PropTypes.number.isRequired,
};

export default ProgressBar;
