import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { makeIcon } from 'core/components/PoPIcon';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import QrCodeScanOverlay from 'core/components/QrCodeScanOverlay';
import { Hash } from 'core/objects';
import { Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { PoPchaHooks } from '../hooks';
import { PoPchaFeature } from '../interface';
import { sendPopchaAuthRequest } from '../network/PopchaMessageApi';

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

/**
 * A scanner to scan QR code to send an authentication request
 */
const PoPchaScanner = () => {
  const laoId = PoPchaHooks.useCurrentLaoId();
  const generateToken = PoPchaHooks.useGenerateToken;

  const [showScanner, setShowScanner] = useState(false);
  const [textScanned, setTextScanned] = useState('');

  const toast = useToast();

  /**
   * Display a toast warning message
   * @param message the message to display
   */
  const showErrorMessage = (message: string) => {
    toast.show(message, {
      type: 'warning',
      placement: 'bottom',
      duration: FOUR_SECONDS,
    });
  };

  /**
   * Verify the scanned info (url)
   * @param data the scanned data
   * @returns true if the scanned info is valid, false otherwise
   */
  const verifyScannedInfo = (data: string) => {
    let url: URL;
    try {
      url = new URL(data);
    } catch (e) {
      console.log(`Error with scanned url: ${e}`);
      showErrorMessage('Invalid url');
      return false;
    }

    const urlArg = url.searchParams;

    const requiredArguments = [
      'client_id',
      'redirect_uri',
      'login_hint',
      'nonce',
      'response_type',
      'scope',
    ];

    // Check if all required arguments are present
    for (const arg of requiredArguments) {
      if (!urlArg.has(arg)) {
        showErrorMessage(`Missing argument ${arg}`);
        return false;
      }
    }

    // Check if the response respects openid standard
    if (urlArg.get('response_type') !== 'id_token token') {
      showErrorMessage('Invalid response type');
      return false;
    }

    if (!(urlArg.get('scope')!.includes('openid') && urlArg.get('scope')!.includes('profile'))) {
      showErrorMessage('Invalid scope');
      return false;
    }

    if (urlArg.has('response_mode')) {
      if (
        !(
          urlArg.get('response_mode')!.includes('query') ||
          urlArg.get('response_mode')!.includes('fragment')
        )
      ) {
        showErrorMessage('Invalid response mode');
        return false;
      }
    }

    if (urlArg.get('login_hint') !== laoId.toString()) {
      showErrorMessage('Invalid lao id');
      console.log(`Scanned lao id: ${urlArg.get('login_hint')}, current lao id: ${laoId}`);
      return false;
    }

    return true;
  };

  /**
   * Send an auth request to the server
   * @param data the scanned data
   * @returns true if the auth request was sent successfully, false otherwise
   */
  const sendAuthRequest = async (data: string) => {
    if (!verifyScannedInfo(data)) {
      return false;
    }

    const url = new URL(data);
    const urlArg = url.searchParams;

    const authResponse = sendPopchaAuthRequest(
      Hash.fromString(urlArg.get('client_id')!),
      urlArg.get('nonce')!,
      urlArg.get('redirect_uri')!,
      urlArg.get('state'),
      urlArg.get('response_mode'),
      laoId,
      generateToken,
    );

    return authResponse
      .then(() => {
        return true;
      })
      .catch((error) => {
        showErrorMessage(`Could not send auth request: ${error}`);
        return false;
      });
  };

  return (
    <>
      <QrCodeScanner
        showCamera={showScanner}
        handleScan={(data: string | null) =>
          data && sendAuthRequest(data).then((success) => success && setTextScanned(data))
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
