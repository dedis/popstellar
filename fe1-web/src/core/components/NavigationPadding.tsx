import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';

import { Icon } from 'core/styles';

/**
 * Returns an empty react component that takes
 * the same amount of horizontal space as n icons would
 * This way it allows proper centering of navigation titles
 */
const NavigationPadding = ({ paddingAmount, nextToIcon }: IPropTypes) => {
  // default is a padding of 1
  const actualPaddingAmount = paddingAmount || 1;

  if (actualPaddingAmount === 0) {
    return null;
  }

  if (nextToIcon) {
    // n times icon size, n-1 times margin, n = actualPaddingAmount
    return (
      <View
        style={{
          width: Icon.size * actualPaddingAmount + Icon.buttonMargin * (actualPaddingAmount - 1),
        }}
      />
    );
  }

  // if there already is an icon, n times icon size and margin
  return <View style={{ width: (Icon.size + Icon.buttonMargin) * actualPaddingAmount }} />;
};

const propTypes = {
  paddingAmount: PropTypes.number,
  nextToIcon: PropTypes.bool,
};

NavigationPadding.propTypes = propTypes;

NavigationPadding.defaultProps = {
  paddingAmount: 1,
  // if this padding is next to an icon, the amount of padding will be different
  nextToIcon: true,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default NavigationPadding;
