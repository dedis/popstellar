import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import QRCodeDisplay from 'react-qr-code';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'center',
  } as ViewStyle,
});

const qrCodeStyles = { height: 'auto', maxWidth: '300px', width: '100%' };

/**
 * Creates and displays a QR code with the public key of the current user
 */
const QRCode = (props: IPropTypes) => {
  const { value, visibility } = props;

  // Displays a QR code with the Public key of the current user
  return visibility ? (
    <View style={styles.container}>
      {/* The typescript prop type for QRCodeDisplay is wrong
        @ts-ignore */}
      <QRCodeDisplay value={value} size={256} style={qrCodeStyles} viewBox="0 0 256 256" />
    </View>
  ) : null;
};

const propTypes = {
  visibility: PropTypes.bool.isRequired,
  value: PropTypes.string,
};
QRCode.propTypes = propTypes;

QRCode.defaultProps = {
  value: '',
};

type IPropTypes = {
  visibility: boolean;
  value: string;
};

export default QRCode;
