import PropTypes from 'prop-types';
import React from 'react';
import { View } from 'react-native';

let onBarCodeScanned: Function | null | undefined = null;

export const fireScan = (x: any) => onBarCodeScanned && onBarCodeScanned(x);

export enum CameraType {
  front = 'front',
  back = 'back',
}

const Camera = (props: IPropTypes) => {
  ({ onBarCodeScanned } = props);
  return <View />;
};

Camera.requestCameraPermissionsAsync = () => Promise.resolve({ status: 'granted' });

const propTypes = {
  onBarCodeScanned: PropTypes.func,
};
Camera.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;
export default Camera;
