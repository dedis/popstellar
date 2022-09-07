import { Camera, CameraType } from 'expo-camera';
import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { getNavigator } from 'core/platform/Navigator';
import { Border, Color, Icon, Spacing } from 'core/styles';

import PoPIcon from './PoPIcon';
import PoPTouchableOpacity from './PoPTouchableOpacity';

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
  flipButtonContainer: {
    ...QrCodeScannerUIElementContainer,
    alignSelf: 'flex-end',
  } as ViewStyle,
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
  const [hasPermission, setHasPermission] = useState(null as unknown as boolean);
  const [cameraType, setCameraType] = useState<CameraType>(CameraType.back);
  const [hasMultipleCameras, setHasMultipleCameras] = useState(false);

  useEffect(() => {
    (async () => {
      const { status } = await Camera.requestCameraPermissionsAsync();
      setHasPermission(status === 'granted');
    })();
  }, []);

  useEffect(() => {
    try {
      getNavigator()
        .mediaDevices.enumerateDevices()
        .then((devices) => {
          console.log(devices.filter((device) => device.kind === 'videoinput'));
          if (devices.filter((device) => device.kind === 'videoinput').length > 1) {
            setHasMultipleCameras(true);
          }
        })
        .catch(console.error);
    } catch (e) {
      // the browser might not support this api
    }
  }, []);

  if (hasPermission === null) {
    return <Text>Requesting for camera permission</Text>;
  }

  if (!hasPermission) {
    return <Text>Permission for camera denied</Text>;
  }

  return (
    <View style={styles.container}>
      <View style={styles.camera}>
        {showCamera && (
          <Camera
            barCodeScannerSettings={{
              barCodeTypes: ['qr'],
            }}
            onBarCodeScanned={handleScan}
          />
        )}
      </View>
      <View style={styles.uiContainer}>
        <View style={styles.children}>{children}</View>
        {hasMultipleCameras && (
          <View style={styles.buttonContainer}>
            <View style={styles.flipButtonContainer}>
              <PoPTouchableOpacity
                style={styles.flipButton}
                onPress={() => {
                  setCameraType(
                    cameraType === CameraType.back ? CameraType.front : CameraType.back,
                  );
                }}>
                <PoPIcon name="cameraReverse" color={Color.accent} size={Icon.size} />
              </PoPTouchableOpacity>
            </View>
          </View>
        )}
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
