import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, TouchableOpacity, View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import QrReader from 'react-qr-reader';

import { Colors, Spacing } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';

import CameraReverseIcon from './icons/CameraReverseIcon';

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
  buttonContainer: {
    flex: 1,
    flexDirection: 'column',
    margin: Spacing.horizontalContentSpacing,
  },
  flipButton: {
    alignSelf: 'flex-end',
    marginLeft: 'auto',
  },
  children: {
    bottom: 0,
    flex: 1,
  },
});

const Camera = ({ showCamera, children, handleScan }: IPropTypes) => {
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
          style={{ width: '100%', position: 'absolute', top: '50%', transform: 'translateY(-50%)' }}
        />
      </View>
      <View style={styles.buttonContainer}>
        <View style={styles.children}>{children}</View>
        <TouchableOpacity
          style={styles.flipButton}
          onPress={() => {
            setFacingMode(facingMode === 'user' ? 'environment' : 'user');
          }}>
          <CameraReverseIcon color={Colors.primary} size={25} />
        </TouchableOpacity>
      </View>
    </View>
  );
};

const propTypes = {
  children: PropTypes.node,
  showCamera: PropTypes.bool.isRequired,
  handleScan: PropTypes.func.isRequired,
};
Camera.propTypes = propTypes;
Camera.defaultProps = {
  children: null,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Camera;
