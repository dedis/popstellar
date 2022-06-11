import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import { Typography } from '../styles';
import PoPButton from './PoPButton';

const PoPTextButton = (props: IPropTypes) => {
  const { onPress, disabled, children: text, negative, testID } = props;

  return (
    <PoPButton onPress={onPress} disabled={disabled} negative={negative} testID={testID}>
      <Text style={[Typography.base, Typography.centered, Typography.negative]}>{text}</Text>
    </PoPButton>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  negative: PropTypes.bool,
  children: PropTypes.string.isRequired,
  testID: PropTypes.string,
};
PoPTextButton.propTypes = propTypes;

PoPTextButton.defaultProps = {
  disabled: false,
  negative: false,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PoPTextButton;
