import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';

import { Icon } from 'core/styles';

const NavigationPadding = ({ n: nInput, onlyPadding }: IPropTypes) => {
  // default is n = 1
  const n = nInput || 1;

  if (n === 0) {
    return null;
  }

  if (onlyPadding) {
    // n times icon size, n-1 times margin
    return <View style={{ width: Icon.size * n + Icon.buttonMargin * (n - 1) }} />;
  }

  // if there already is an icon, n times icon size and margin
  return <View style={{ width: (Icon.size + Icon.buttonMargin) * n }} />;
};

const propTypes = {
  n: PropTypes.number,
  onlyPadding: PropTypes.bool,
};

NavigationPadding.propTypes = propTypes;

NavigationPadding.defaultProps = {
  n: 1,
  onlyPadding: true,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default NavigationPadding;

/**
 * This function returns an empty react component that takes
 * the same amount of horizontal space as n icons would
 * Hence it allows proper centering of navigation titles
 * @param n The number of units of padding that should be added
 * @returns The component that can be used for padding
 */
export const makeNavigationPadding = (n: number) => () => {
  return <NavigationPadding n={n} />;
};
