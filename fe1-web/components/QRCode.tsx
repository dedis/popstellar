import React from 'react';
import PropTypes from 'prop-types';
import { KeyPairStore } from 'store/stores';
import QRCodeDisplay from 'qrcode.react';
import STRINGS from '../res/strings';
import TextBlock from './TextBlock';

const styles = {
  textAlign: 'center',
  padding: 10,
};

const QRCode = (props: IPropTypes) => {
  const identity = KeyPairStore.getPublicKey().toString();
  const { visibility } = props;
  const { size } = props;

  // Displays a QR code with the Public key of the current user
  return (visibility)
    ? [
      <TextBlock text={STRINGS.identity_qrcode_description} />,
      <div style={styles}>
        <QRCodeDisplay value={identity} size={size} />
      </div>,
    ]
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

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default QRCode;
