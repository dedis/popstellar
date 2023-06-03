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

/**
 * Attention: react-camera currently has the problem that the onBarCodeScanned property is only set initially, i.e
 * the function cannot be changed later meaning if we pass a function that references other variables, they will
 * only have their proper values if they are mutated which in general should be avoided.
 * (2022-12-05, Tyratox) Issue to track this: https://github.com/dedis/popstellar/issues/1306
 */
const QrCodeScanner = ({ showCamera, children, handleScan }: IPropTypes) => {
  // On future releases this might be changed to allow switching cameras. But for now this is not working. (2023-06-03)
  const hasMultipleCameras = false;
  const [cameraType, setCameraType] = useState<CameraType>(CameraType.back);
  const [hasCamera, setHasCamera] = useState(true);
  const [permission] = Camera.useCameraPermissions();

  useEffect(() => {
    (async () => {
      if (permission && permission.granted) {
        const isAvailable = await Camera.isAvailableAsync();
        setHasCamera(isAvailable);
        if (isAvailable) {
          const types = await Camera.getAvailableCameraTypesAsync();
          setCameraType(types.includes(CameraType.back) ? CameraType.back : CameraType.front);
        }
      }
    })();
  }, [permission]);

  if (!permission) {
    return (
      <View style={styles.container}>
        <Text>{STRINGS.requesting_camera_permissions}</Text>
        <View style={styles.children}>{children}</View>
      </View>
    );
  }

  if (!permission.granted) {
    return (
      <View style={styles.container}>
        <Text>{STRINGS.camera_permissions_denied}</Text>
        <View style={styles.children}>{children}</View>
      </View>
    );
  }

  if (!hasCamera) {
    return (
      <View style={styles.container}>
        <Text>{STRINGS.camera_unavailable}</Text>
        <View style={styles.children}>{children}</View>
      </View>
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
