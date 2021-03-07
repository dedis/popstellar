import React from 'react';
import PropTypes from 'prop-types';
import { KeyPairStore } from 'store/stores';
import QRCodeDisplay from 'qrcode.react';
import { View } from 'react-native';
import styleContainer from 'styles/stylesheets/container';
import TextBlock from './TextBlock';

/**
 * Creates and displays a QR code with the public key of the current user
 */

const QRCode = (props: IPropTypes) => {
  const identity = KeyPairStore.getPublicKey().toString();
  const { visibility } = props;
  const { size } = props;
  const { text } = props;

  // Displays a QR code with the Public key of the current user
  return (visibility)
    ? (
      <>
        <TextBlock text={String(text)} />
        <View style={[styleContainer.topCenter, { padding: 10 }]}>
          <QRCodeDisplay value={identity} size={Number(size)} />
        </View>
      </>
    )
    : null;
};

const propTypes = {
  visibility: PropTypes.bool.isRequired,
  size: PropTypes.number,
  text: PropTypes.string,
};
QRCode.propTypes = propTypes;

QRCode.defaultProps = {
  size: 160, // Size of the QR code in pixels
  text: '', // Default is not text
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default QRCode;
