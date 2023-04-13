import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { makeIcon } from '../../../core/components/PoPIcon';
import PoPTouchableOpacity from '../../../core/components/PoPTouchableOpacity';
import QrCodeScanner, {
  QrCodeScannerUIElementContainer,
} from '../../../core/components/QrCodeScanner';
import QrCodeScanOverlay from '../../../core/components/QrCodeScanOverlay';
import { Typography } from '../../../core/styles';
import STRINGS from '../../../resources/strings';
import { PoPchaHooks } from '../hooks';
import { PoPchaFeature } from '../interface';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-between',
    alignItems: 'center',
  } as ViewStyle,
  qrCode: {
    opacity: 0.5,
  } as ViewStyle,
});
const PoPchaScanner = () => {
  const laoId = PoPchaHooks.useCurrentLaoId();

  const [showScanner, setShowScanner] = useState(false);
  const [textScanned, setTextScanned] = useState('');

  const verifyScannedInfo = (data: string) => {
    setTextScanned(data);
    return true;
  };

  return (
    <>
      <QrCodeScanner
        showCamera={showScanner}
        handleScan={(data: string | null) =>
          data && verifyScannedInfo(data) && setShowScanner(false)
        }>
        <View style={styles.container}>
          <View>
            <Text style={Typography.paragraph}>Hello, here is your laoID: {laoId}</Text>
            <text>{textScanned}</text>
          </View>
          {showScanner && (
            <View style={styles.qrCode}>
              <QrCodeScanOverlay width={300} height={300} />
            </View>
          )}
          <View>
            <View style={QrCodeScannerUIElementContainer}>
              <PoPTouchableOpacity
                testID="popcha_scanner_button"
                onPress={() => setShowScanner(!showScanner)}>
                <Text style={[Typography.base, Typography.accent]}>
                  {showScanner ? 'Close Scanner' : 'Open Scanner'}
                </Text>
              </PoPTouchableOpacity>
            </View>
          </View>
        </View>
      </QrCodeScanner>
    </>
  );
};

export default PoPchaScanner;

export const popchaScannerScreen: PoPchaFeature.LaoScreen = {
  id: STRINGS.navigation_lao_popcha,
  Icon: makeIcon('scan'),
  Component: PoPchaScanner,
  order: 100000,
};
