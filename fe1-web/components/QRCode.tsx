import React from 'react';
import PropTypes from 'prop-types';
import { KeyPairStore } from 'store/stores';
import QRCodeDisplay from 'qrcode.react';
import { View } from 'react-native';
import styleContainer from 'styles/stylesheets/container';

/**
 * Creates and displays a QR code with the public key of the current user
 */

const QRCode = (props: IPropTypes) => {
  const identity = KeyPairStore.getPublicKey().toString();
  const { visibility } = props;
  const { size } = props;

  // Displays a QR code with the Public key of the current user
  return (visibility)
    ? (
      <View style={[styleContainer.anchoredCenter, { padding: 10, justifyContent: 'flex-start' }]}>
        <QRCodeDisplay value={identity} size={Number(size)} />
      </View>
    )
    : null;
};

const propTypes = {
  visibility: PropTypes.bool.isRequired,
  size: PropTypes.number,
};
QRCode.propTypes = propTypes;

QRCode.defaultProps = {
  size: 160, // Size of the QR code in pixels
};

type IPropTypes = {
  visibility: boolean,
  size: number,
};

export default QRCode;
