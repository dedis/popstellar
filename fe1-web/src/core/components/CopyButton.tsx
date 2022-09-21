import * as Clipboard from 'expo-clipboard';
import PropTypes from 'prop-types';
import React from 'react';
import { Pressable } from 'react-native';

import { Color } from 'core/styles';

import PoPIcon from './PoPIcon';

/**
 * Copy to clipboard button
 */

function CopyButton({ data, negative }: IPropTypes) {
  const copyToClipBoard = async () => {
    await Clipboard.setStringAsync(data);
  };

  return (
    <Pressable onPress={copyToClipBoard}>
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
