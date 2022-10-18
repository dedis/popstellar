import PropTypes from 'prop-types';
import React from 'react';
import { Text } from 'react-native';

import { Typography } from '../styles';
import PoPButton from './PoPButton';

const PoPTextButton = (props: IPropTypes) => {
  const { onPress, disabled, children: text, outline, negativeBorder, margin, testID } = props;

  return (
    <PoPButton
      onPress={onPress}
      disabled={disabled}
      outline={outline}
      negativeBorder={negativeBorder}
      testID={testID}
      margin={margin}>
      <Text
        style={
          outline
            ? [Typography.base, Typography.centered, Typography.accent]
            : [Typography.base, Typography.centered, Typography.negative]
        }>
        {text}
      </Text>
    </PoPButton>
  );
};

const propTypes = {
  onPress: PropTypes.func.isRequired,
  disabled: PropTypes.bool,
  outline: PropTypes.bool,
  negativeBorder: PropTypes.bool,
  margin: PropTypes.bool,
  children: PropTypes.string.isRequired,
  testID: PropTypes.string,
};
PoPTextButton.propTypes = propTypes;

PoPTextButton.defaultProps = {
  disabled: false,
  outline: false,
  negativeBorder: false,
  margin: true,
  testID: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default PoPTextButton;
