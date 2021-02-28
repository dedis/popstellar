import React from 'react';
import * as QRCode from 'qrcode.react';
import {
  View,
} from 'react-native';
import PropTypes from 'prop-types';

/**
 * QR code button that executes an onPress action given in props
 */

// function QRCodeDisplay() {
//   return (
//     <View>
//       <h1>Your Public Key</h1>
//       <QRCode value="adalsteinn.ml" id="testId" />
//     </View>
//   );
// }

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
