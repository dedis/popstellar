import React from 'react';
import PropTypes from 'prop-types';
import { KeyPairStore } from 'store/stores';
import QRCodeDisplay from 'qrcode.react';
import TextBlock from './TextBlock';

/**
 * Creates and displays a QR code with the public key of the current user
 */

const styles = {
  textAlign: 'center',
  padding: 10,
};

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
        <div style={styles}>
          <QRCodeDisplay value={identity} size={Number(size)} />
        </div>
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
