import { BarCodeScanningResult, Camera, CameraType } from 'expo-camera';
import PropTypes from 'prop-types';
import React, { useEffect, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { Border, Color, Icon, Spacing } from 'core/styles';
import STRINGS from 'resources/strings';

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
  const [permission, requestPermission] = Camera.useCameraPermissions();
  const [cameraType, setCameraType] = useState<CameraType>(CameraType.back);
  const [hasMultipleCameras, setHasMultipleCameras] = useState(true);
  const [hasCamera, setHasCamera] = useState(true);

  useEffect(() => {
    (async () => {
      if (permission && !permission.granted) {
        await requestPermission();
      }
    })();
  }, [permission, requestPermission]);

  useEffect(() => {
    let isMounted = true;
    (async () => {
      const isAvailable = await Camera.isAvailableAsync();
      if (isMounted) {
        setHasCamera(isAvailable);
      } else {
        return;
      }
      if (isAvailable) {
        const types = await Camera.getAvailableCameraTypesAsync();
        if (isMounted) {
          setHasMultipleCameras(types.length > 1);
        }
      }
    })();
    return () => {
      isMounted = false;
    };
  }, []);

  if (!hasCamera) {
    return (
      <>
        <Text>{STRINGS.camera_unavailable}</Text>
        <View style={styles.children}>{children}</View>
      </>
    );
  }

  if (!permission) {
    return (
      <>
        <Text>{STRINGS.requesting_camera_permissions}</Text>
        <View style={styles.children}>{children}</View>
      </>
    );
  }

  if (!permission.granted) {
    return (
      <>
        <Text>{STRINGS.camera_permissions_denied}</Text>
        <View style={styles.children}>{children}</View>
      </>
    );
  }

  // Scan each code only once
  let lastValue: string;
  const onBarCodeScanned = (result: BarCodeScanningResult) => {
    if (lastValue && lastValue === result.data) {
      return;
    }
    handleScan(result.data);
    lastValue = result.data;
  };

  return (
    <View style={styles.container}>
      <View style={styles.camera}>
        {showCamera && (
          <Camera
            barCodeScannerSettings={{
              barCodeTypes: ['qr'],
            }}
            onBarCodeScanned={onBarCodeScanned}
            type={cameraType}
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
