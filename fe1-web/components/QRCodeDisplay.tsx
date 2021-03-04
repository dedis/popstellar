import React from 'react';
import * as QRCode from 'qrcode.react';

import PropTypes from 'prop-types';

/**
 * QR code that shows the value that is provided, optional parameter: size (in pixels)
 */


const QRCodeDisplay = (props: IPropTypes) => {
  const { value } = props;
  const { size } = props;
  return (
    <QRCode value={value} size={size} />
  );
};

const propTypes = {
  value: PropTypes.string.isRequired,
  size: PropTypes.number,
};
QRCodeDisplay.prototype = propTypes;

QRCodeDisplay.defaultProps = {
  size: 128,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default QRCodeDisplay;
