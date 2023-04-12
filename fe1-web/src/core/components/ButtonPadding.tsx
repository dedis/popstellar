import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';

import { Icon } from 'core/styles';

/**
 * Returns an empty react component that takes
 * the same amount of horizontal space as n icons would
 * This way it allows proper centering of navigation titles
 */
const ButtonPadding = ({ paddingAmount, nextToIcon }: IPropTypes) => {
  // default is a padding of 1
  const actualPaddingAmount =
    paddingAmount === undefined || paddingAmount === null ? 1 : paddingAmount;

  if (actualPaddingAmount === 0) {
    return null;
  }

  if (nextToIcon) {
    // if there already is an icon, n times icon size and margin
    return <View style={{ width: (Icon.size + Icon.buttonMargin) * actualPaddingAmount }} />;
  }

  // n times icon size, n-1 times margin, n = actualPaddingAmount
  return (
    <View
      style={{
        width: Icon.size * actualPaddingAmount + Icon.buttonMargin * (actualPaddingAmount - 1),
      }}
    />
  );
};

const propTypes = {
  paddingAmount: PropTypes.number,
  nextToIcon: PropTypes.bool,
};

ButtonPadding.propTypes = propTypes;

ButtonPadding.defaultProps = {
  paddingAmount: 1,
  // if this padding is next to an icon, the amount of padding will be different
  nextToIcon: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ButtonPadding;
