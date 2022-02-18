import React from 'react';
import PropTypes from 'prop-types';
import { View } from 'react-native';

let onScan: Function | null | undefined = null;
let onError: Function | null | undefined = null;

export const fireScan = (x: any) => onScan && onScan(x);
export const fireError = (x: any) => onError && onError(x);

const QrReader = (props: IPropTypes) => {
  ({ onScan, onError } = props);
  return <View />;
};

const propTypes = {
  onScan: PropTypes.func,
  onError: PropTypes.func,
};
QrReader.prototype = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;
export default QrReader;
