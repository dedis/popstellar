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
      We are only allowed to cover < 30% of the data (lets say 25% to be safe) for
      error correction level H (https://www.qrcode.com/en/about/error_correction.html)
      Following https://www.maketecheasier.com/assets/uploads/2019/07/qr-code-anatomy-overview.png.webp
      and https://www.qrcode.com/en/about/version.html for version 7 only 1583 squares of 2025 are used to store data.
      This leads to 25 * 1583 = 2025 * x where x is the max covered size in terms of QrCode surface percentage.
      x = 19,543 % = 0.19543. Since we need the result for a line given the squared proportion we can cover
      sqrt(0.19543) = 0.442 => (100-44.2) / 2 = 27.9

      The above computation takes into account the area covered by
      position, alignment and timing patterns; and version and format information; which are not data
    */
    position: 'absolute',
    padding: '4',
    top: '27.9%',
    left: '27.9%',
    right: '27.9%',
    bottom: '27.9%',
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

  // Warns that the overlay is too big if the text is too small
  if (overlayText && value && value.length < 59) {
    console.warn(
      'An overlay text has been added on a QRCode whose represents a too short text (length < 50)',
    );
  }

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
