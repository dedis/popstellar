import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, TextStyle } from 'react-native';

import { Typography } from '../styles';
import { black } from '../styles/colors';

/**
 * Block of text that gets displayed
 * Required input: text
 * Optional input: bold, visibility (both bool) and color (string)
 */

const TextBlock = (props: IPropTypes) => {
  const { text, bold, visibility, color } = props;

  if (!visibility) {
    return null;
  }

  const fontStyle = bold ? Typography.importantCentered : Typography.baseCentered;
  const styles = StyleSheet.create({
    text: {
      ...fontStyle,
      color: color,
    } as TextStyle,
  });

  return <Text style={styles.text}>{text}</Text>;
};

const propTypes = {
  text: PropTypes.string.isRequired,
  bold: PropTypes.bool,
  visibility: PropTypes.bool,
  color: PropTypes.string,
};
TextBlock.propTypes = propTypes;

TextBlock.defaultProps = {
  bold: false,
  visibility: true,
  color: black,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextBlock;
