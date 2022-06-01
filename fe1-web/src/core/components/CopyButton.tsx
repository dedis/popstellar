import PropTypes from 'prop-types';
import React from 'react';
import { Pressable } from 'react-native';

import { Color } from 'core/styles';

import { getNavigator } from '../platform/Navigator';
import PoPIcon from './PoPIcon';

/**
 * Copy to clipboard button
 */

function CopyButton({ data, negative }: IPropTypes) {
  return (
    <Pressable onPress={() => getNavigator().clipboard.writeText(data)}>
      <PoPIcon name="copy" color={negative ? Color.contrast : Color.primary} size={26} />
    </Pressable>
  );
}

const propTypes = {
  data: PropTypes.string.isRequired,
  negative: PropTypes.bool,
};

CopyButton.propTypes = propTypes;

CopyButton.defaultProps = {
  negative: false,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default CopyButton;
