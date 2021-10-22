import React from 'react';
import { StyleSheet, Text, TextStyle } from 'react-native';
import PropTypes from 'prop-types';
import { Typography } from 'styles';

/**
 * Block of text that gets displayed
 * Required input: text
 * Optional input: bold, visibility (both bool) and color (string)
 */

const TextBlock = (props: IPropTypes) => {
  const { text } = props;
  const { bold } = props;
  const { visibility } = props;
  const { color } = props;

  const styles = StyleSheet.create({
    textStandard: {
      ...Typography.base,
      color: color,
    } as TextStyle,
    textBold: {
      ...Typography.important,
      color: color,
    } as TextStyle,
  });

  if (visibility) {
    if (bold) {
      return <Text style={styles.textBold}>{text}</Text>;
    }
    return <Text style={styles.textStandard}>{text}</Text>;
  }
  return null;
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
  color: 'black',
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextBlock;
