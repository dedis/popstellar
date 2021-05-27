import React from 'react';
import {
  StyleSheet, Image, Pressable, ImageStyle,
} from 'react-native';
import PropTypes from 'prop-types';

const copyIcon = require('res/img/copy.svg');

/**
 * Copy to clipboard button
 */

const styles = StyleSheet.create({
  icon: {
    width: 26,
    height: 26,
    cursor: 'pointer',
  } as ImageStyle,
});

function CopyButton({ action }: IPropTypes) {
  return (
    <Pressable onPress={action}>
      <Image style={styles.icon} source={copyIcon} />
    </Pressable>
  );
}

const propTypes = {
  action: PropTypes.func.isRequired,
};
CopyButton.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CopyButton;
