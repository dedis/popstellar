import React, { useState } from 'react';
import { StyleSheet, ViewStyle } from 'react-native';

import { makeIcon } from 'core/components/PoPIcon';
import STRINGS from 'resources/strings';

import PopchaScannerClosed from '../components/PopchaScannerClosed';
import PopchaScannerOpen from '../components/PopchaScannerOpen';
import { PopchaFeature } from '../interface';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    alignItems: 'center',
  } as ViewStyle,
});

/**
 * A scanner to scan QR code to send an authentication request
 */
const PopchaScanner = () => {
  const [showScanner, setShowScanner] = useState(false);

  return showScanner ? (
    <PopchaScannerOpen onClose={() => setShowScanner(true)} containerStyle={styles.container} />
  ) : (
    <PopchaScannerClosed
      onOpenPress={() => setShowScanner(true)}
      containerStyle={styles.container}
    />
  );
};

export default PopchaScanner;

export const PopchaScannerScreen: PopchaFeature.LaoScreen = {
  id: STRINGS.navigation_lao_popcha,
  Icon: makeIcon('key'),
  Component: PopchaScanner,
  order: 100000,
};
