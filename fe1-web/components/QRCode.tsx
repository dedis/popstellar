import React from 'react';
import PropTypes from 'prop-types';
import TextBlock from 'components/TextBlock';
import { KeyPairStore } from 'store/stores';

const QRCode = (props: IPropTypes) => {
  const identity = KeyPairStore.getPublicKey().toString();
  const { visibility } = props;

  // For now, we simply display a text block with the public key
  return (visibility)
    ? <TextBlock bold text={`ID (pk) :   ${identity}`} />
    : null;
};

const propTypes = {
  visibility: PropTypes.bool.isRequired,
};
QRCode.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default QRCode;
