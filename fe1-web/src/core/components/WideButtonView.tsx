import PropTypes from 'prop-types';
import React from 'react';
import { Button, StyleSheet, View, ViewStyle } from 'react-native';

import { Colors, Spacing } from '../styles';

const styles = StyleSheet.create({
  wideButtonView: {
    marginVertical: Spacing.x1,
  } as ViewStyle,
  button: {} as ViewStyle,
});

const WideButtonView = (props: IPropTypes) => {
  const { title, onPress, disabled } = props;

  return (
    <View style={styles.wideButtonView}>
      <Button title={title} onPress={onPress} disabled={!!disabled} color={Colors.primary} />
    </View>
  );
};

const propTypes = {
  title: PropTypes.string.isRequired,
  onPress: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
};
WideButtonView.propTypes = propTypes;

WideButtonView.defaultProps = {
  disabled: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WideButtonView;
