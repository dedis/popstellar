import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import { Typography } from '../styles';
import PoPButton from './PoPButton';

const PoPTextButton = (props: IPropTypes) => {
  const { onPress, disabled, children: text, negative } = props;

  return (
    <PoPButton onPress={onPress} disabled={disabled} negative={negative}>
      <Text style={[Typography.base, Typography.centered, Typography.negative]}>{text}</Text>
    </PoPButton>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  negative: PropTypes.bool,
  children: PropTypes.string.isRequired,
};
PoPTextButton.propTypes = propTypes;

PoPTextButton.defaultProps = {
  disabled: false,
  negative: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PoPTextButton;
