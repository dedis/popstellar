import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import QRCodeDisplay from 'react-qr-code';

import { Color, Typography } from 'core/styles';

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    justifyContent: 'center',
  } as ViewStyle,
  innerContainer: {
    position: 'relative',
  },
  overlay: {
    /*
      we are only allowed to cover < 30% (lets say 25% to be safe) for
      error correction level H (https://www.qrcode.com/en/about/error_correction.html)
      assuming the qr code is a square, this means we can cover a width of 50%
      and a height of 50%. centering this gives us 25% margin on every side

      The above computation might not take into account the area covered by the mouse
      and some squares that are partially hidden by the overlay (MeKHell, 13.04.2023)
    */
    position: 'absolute',
    padding: '4',
    top: '26%',
    left: '26%',
    right: '26%',
    bottom: '26%',
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Color.accent,
    overflow: 'hidden',
    borderRadius: 100,
  },
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
      <View style={styles.innerContainer}>
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
        {overlayText && (
          <View style={styles.overlay}>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              {overlayText}
            </Text>
          </View>
        )}
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
