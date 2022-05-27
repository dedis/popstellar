import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, TouchableOpacity, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import QrReader from 'react-qr-reader';

import { Border, Color, Icon, Spacing } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';

import CameraReverseIcon from './icons/CameraReverseIcon';

// FIXME: Remove CSS imports in order to support native apps
// At the time of writing expo-camera nor expo-barcode-scanner work in web builds
// because they load an external dependency (jsQR) that somehow does not properly load
// outside the examples expo provides
import '../platform/web-styles/qr-code-scanner.css';

export const QrCodeScannerUIElementContainer: ViewStyle = {
  backgroundColor: Color.contrast,
  padding: Spacing.x05,
  borderRadius: Border.radius,
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    overflow: 'hidden',
  },
  camera: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    zIndex: 0,
  },
  uiContainer: {
    flex: 1,
    flexDirection: 'column',
    margin: Spacing.contentSpacing,
  },
  buttonContainer: {
    flexDirection: 'column',
  },
  flipButtonContainer: { ...QrCodeScannerUIElementContainer, alignSelf: 'flex-end' } as ViewStyle,
  flipButton: {
    alignSelf: 'flex-end',
    marginLeft: 'auto',
  },
  children: {
    bottom: 0,
    flex: 1,
  },
});

const QrCodeScanner = ({ showCamera, children, handleScan }: IPropTypes) => {
  const toast = useToast();
  const [facingMode, setFacingMode] = useState<'user' | 'environment'>('user');

  const handleError = (err: string) => {
    console.error(err);
    toast.show(err, {
      type: 'danger',
      placement: 'top',
      duration: FOUR_SECONDS,
    });
  };

  if (!showCamera) {
    return null;
  }

  return (
    <View style={styles.container}>
      <View style={styles.camera}>
        <QrReader
          delay={300}
          onError={handleError}
          onScan={handleScan}
          facingMode={facingMode}
          className="qr-code-scanner"
        />
      </View>
      <View style={styles.uiContainer}>
        <View style={styles.children}>{children}</View>
        <View style={styles.buttonContainer}>
          <View style={styles.flipButtonContainer}>
            <TouchableOpacity
              style={styles.flipButton}
              onPress={() => {
                setFacingMode(facingMode === 'user' ? 'environment' : 'user');
              }}>
              <CameraReverseIcon color={Color.accent} size={Icon.size} />
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </View>
  );
};

const propTypes = {
  children: PropTypes.node,
  showCamera: PropTypes.bool.isRequired,
  handleScan: PropTypes.func.isRequired,
};
QrCodeScanner.propTypes = propTypes;
QrCodeScanner.defaultProps = {
  children: null,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default QrCodeScanner;
