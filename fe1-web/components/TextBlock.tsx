import React from 'react';
import { StyleSheet, Text, TextStyle } from 'react-native';
import PropTypes from 'prop-types';
import { Typography } from 'styles';

/**
 * Block of text that gets displayed
 * Required input: text
 * Optional input: bold, visibility (both bool)
 */

const styles = StyleSheet.create({
  textStandard: {
    ...Typography.base,
  } as TextStyle,
  textBold: {
    ...Typography.important,
  } as TextStyle,
});

const TextBlock = (props: IPropTypes) => {
  const { text } = props;
  const { bold } = props;
  const { visibility } = props;
  const textStyle = (bold ? styles.textBold : styles.textStandard);

  return (visibility)
    ? (
      <Text style={textStyle}>{text}</Text>
    ) : null;
};

const propTypes = {
  text: PropTypes.string.isRequired,
  bold: PropTypes.bool,
  visibility: PropTypes.bool,
};
TextBlock.propTypes = propTypes;

TextBlock.defaultProps = {
  bold: false,
  visibility: true,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default TextBlock;
