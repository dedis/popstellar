import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';

let onBarCodeScanned: Function | null | undefined = null;

export const fireScan = (data: string) => onBarCodeScanned && onBarCodeScanned({ data });

export enum CameraType {
  front = 'front',
  back = 'back',
}

export enum PermissionStatus {
  DENIED = 'denied',
}

export type BarCodeScanningResult = {
  data: string;
};

const Camera = (props: IPropTypes) => {
  ({ onBarCodeScanned } = props);
  return <View />;
};

// To match the PermissionResponse interface
const response = {
  canAskAgain: true,
  expires: 'never',
  granted: true,
  status: 'granted',
};

Camera.useCameraPermissions = () => [response, () => Promise.resolve(response)];
Camera.isAvailableAsync = () => Promise.resolve(true);
Camera.getAvailableCameraTypesAsync = () => Promise.resolve([CameraType.front, CameraType.back]);

const propTypes = {
  onBarCodeScanned: PropTypes.func,
};
Camera.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;
export default Camera;
export { Camera };
