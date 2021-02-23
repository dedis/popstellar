import React from 'react';
import {
  Button, StyleSheet, View, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';
import { Spacing } from 'styles';

const styles = StyleSheet.create({
  wideButtonView: {
    marginHorizontal: Spacing.xl,
    marginVertical: Spacing.xs,
  } as ViewStyle,
});

const WideButtonView = (props: IPropTypes) => {
  const { title } = props;
  const { onPress } = props;

  return (
    <View style={styles.wideButtonView}>
      <Button
        title={title}
        onPress={onPress}
      />
    </View>
  );
};

const propTypes = {
  title: PropTypes.string.isRequired,
  onPress: PropTypes.func.isRequired,
};
WideButtonView.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default WideButtonView;
