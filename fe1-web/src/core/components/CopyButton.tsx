import PropTypes from 'prop-types';
import React from 'react';
import { Image, ImageStyle, Pressable, StyleSheet } from 'react-native';

import { getNavigator } from '../platform/Navigator';

const copyIcon = require('resources/img/copy.svg');

/**
 * Copy to clipboard button
 */
const styles = StyleSheet.create({
  icon: {
    width: 26,
    height: 26,
    // we comment cursor to pass the test because this property does not exist for ImageStyle
    // cursor: 'pointer',
  } as ImageStyle,
});

function CopyButton({ data }: IPropTypes) {
  return (
    <Pressable onPress={() => getNavigator().clipboard.writeText(data)}>
      <Image style={styles.icon} source={copyIcon} />
    </Pressable>
  );
}

const propTypes = {
  data: PropTypes.string.isRequired,
};
CopyButton.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CopyButton;
