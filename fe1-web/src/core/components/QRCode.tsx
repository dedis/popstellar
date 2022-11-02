import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import QRCodeDisplay from 'react-qr-code';

import { Border, Color, Spacing, Typography } from 'core/styles';

const styles = StyleSheet.create({
  container: {
    position: 'relative',
    flexDirection: 'row',
    justifyContent: 'center',
  } as ViewStyle,
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
  },
  overlayTextContainer: {
    backgroundColor: Color.accent,
    borderRadius: Border.radius,
    padding: Spacing.x05,
  } as ViewStyle,
});

const qrCodeStyles = { height: 'auto', maxWidth: '300px', width: '100%' };

/**
 * Creates and displays a QR code with the public key of the current user
 */
const QRCode = ({ value, visible, overlayText }: IPropTypes) => {
  // Displays a QR code with the Public key of the current user
  if (!visible) {
    return null;
  }

  // highest error correction if there is an overlay
  const errorCorrectionLevel = overlayText ? 'H' : 'L';

  return (
    <View style={styles.container}>
      <QRCodeDisplay
        value={value || ''}
        size={256}
        style={qrCodeStyles}
        level={errorCorrectionLevel}
        {...{
          /* The typescript prop type for QRCodeDisplay is wrong */
          viewBox: '0 0 256 256',
        }}
      />
      <View style={styles.overlay}>
        <View style={styles.overlayTextContainer}>
          <Text style={[Typography.base, Typography.centered, Typography.negative]}>
            {overlayText}
          </Text>
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  value: PropTypes.string,
  visible: PropTypes.bool,
  overlayText: PropTypes.string,
};
QRCode.propTypes = propTypes;

QRCode.defaultProps = {
  value: '',
  visible: true,
  overlayText: undefined,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default QRCode;
