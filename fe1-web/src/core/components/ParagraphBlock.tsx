import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, TextStyle } from 'react-native';

import { Spacing, Typography } from '../styles';

const styles = StyleSheet.create({
  textStandard: {
    marginHorizontal: Spacing.x1,
  } as TextStyle,
  textBold: {
    ...Typography.importantCentered,
  } as TextStyle,
});

const ParagraphBlock = (props: IPropTypes) => {
  const { text, bold } = props;

  const style = bold ? styles.textBold : styles.textStandard;
  return text !== undefined && text !== null ? <Text style={style}>{text}</Text> : null;
};

const propTypes = {
  text: PropTypes.string.isRequired,
  bold: PropTypes.bool,
};
ParagraphBlock.propTypes = propTypes;

ParagraphBlock.defaultProps = {
  bold: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ParagraphBlock;
