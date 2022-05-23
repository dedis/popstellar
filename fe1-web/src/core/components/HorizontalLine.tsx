import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';

import { Border, Color } from 'core/styles';

const styles = StyleSheet.create({
  hr: {
    borderColor: Color.accent,
    borderWidth: Border.width,
    borderRadius: Border.radius,
  } as ViewStyle,
  negative: {
    borderColor: Color.contrast,
  } as ViewStyle,
});

const HorizontalLine = ({ negative }: IPropTypes) => (
  <View style={negative ? [styles.hr, styles.negative] : styles.hr} />
);

const propTypes = {
  negative: PropTypes.bool,
};
HorizontalLine.propTypes = propTypes;

HorizontalLine.defaultProps = {
  negative: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default HorizontalLine;
