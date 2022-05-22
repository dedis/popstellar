import PropTypes from 'prop-types';
import React from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import QRCodeDisplay from 'react-qr-code';

import containerStyles from '../styles/stylesheets/containerStyles';

const styles = StyleSheet.create({
  container: {
    padding: 10,
    justifyContent: 'flex-start',
  } as ViewStyle,
});

/**
 * Creates and displays a QR code with the public key of the current user
 */
const QRCode = (props: IPropTypes) => {
  const { value, visibility, size } = props;

  // Displays a QR code with the Public key of the current user
  return visibility ? (
    <View style={[containerStyles.anchoredCenter, styles.container]}>
      <QRCodeDisplay value={value} size={size} />
    </View>
  ) : null;
};

const propTypes = {
  visibility: PropTypes.bool.isRequired,
  size: PropTypes.number,
  value: PropTypes.string,
};
QRCode.propTypes = propTypes;

QRCode.defaultProps = {
  size: 160, // Size of the QR code in pixels
  value: '',
};

type IPropTypes = {
  visibility: boolean;
  size: number;
  value: string;
};

export default QRCode;
