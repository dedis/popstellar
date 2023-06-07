import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { ConfirmModal } from 'core/components';
import { makeIcon } from 'core/components/PoPIcon';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import QrCodeScanOverlay from 'core/components/QrCodeScanOverlay';
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { PopchaHooks } from '../hooks';
import { PopchaFeature } from '../interface';
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
  topMargin: {
    marginTop: Spacing.x05,
  } as ViewStyle,
});

/**
 * A scanner to scan QR code to send an authentication request
 */
const PopchaScanner = () => {
  const laoId = PopchaHooks.useCurrentLaoId();
  const generateToken = PopchaHooks.useGenerateToken();

  const [showScanner, setShowScanner] = useState(false);
  const [isInputModalVisible, setInputModalIsVisible] = useState(false);

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
   * Display a toast success message
   * @param message the message to display
   */
  const showSuccessMessage = (message: string) => {
    toast.show(message, {
      type: 'success',
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
    if (urlArg.get('response_type') !== 'id_token') {
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
      urlArg.get('client_id')!,
      urlArg.get('nonce')!,
      url.host,
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

  return showScanner ? (
    <>
      <QrCodeScanner
        showCamera
        handleScan={(data: string | null) =>
          data &&
          sendAuthRequest(data).then((success) => {
            if (success) {
              setShowScanner(false);
              showSuccessMessage(STRINGS.popcha_success_authentication);
            }
          })
        }>
        <View style={styles.container}>
          <View />
          <View style={styles.qrCode}>
            <QrCodeScanOverlay width={300} height={300} />
          </View>
          <View>
            <View style={QrCodeScannerUIElementContainer}>
              <PoPTouchableOpacity
                testID="popcha_add_manually"
                onPress={() => setInputModalIsVisible(true)}>
                <Text style={[Typography.base, Typography.accent, Typography.centered]}>
                  {STRINGS.general_enter_manually}
                </Text>
              </PoPTouchableOpacity>
            </View>
            <View style={[QrCodeScannerUIElementContainer, styles.topMargin]}>
              <PoPTouchableOpacity
                testID="popcha_scanner_button"
                onPress={() => setShowScanner(!showScanner)}>
                <Text style={[Typography.base, Typography.accent]}>
                  {STRINGS.popcha_close_scanner}
                </Text>
              </PoPTouchableOpacity>
            </View>
          </View>
        </View>
      </QrCodeScanner>
      <ConfirmModal
        visibility={isInputModalVisible}
        setVisibility={setInputModalIsVisible}
        title={STRINGS.popcha_manual_add_title}
        description={STRINGS.popcha_manual_add_description}
        onConfirmPress={(text: string) => {
          sendAuthRequest(text).then((success) => {
            if (success) {
              setShowScanner(false);
              setInputModalIsVisible(false);
              showSuccessMessage(STRINGS.popcha_success_authentication);
            }
          });
        }}
        buttonConfirmText={STRINGS.general_add}
        hasTextInput
        textInputPlaceholder={STRINGS.popcha_url_type_input}
      />
    </>
  ) : (
    <>
      <QrCodeScanner showCamera={false} handleScan={() => {}}>
        <View style={styles.container}>
          <View>
            <Text style={Typography.paragraph}>
              {STRINGS.popcha_display_current_lao}
              {laoId}
            </Text>
          </View>
          <View>
            <View style={[QrCodeScannerUIElementContainer, styles.topMargin]}>
              <PoPTouchableOpacity
                testID="popcha_scanner_button"
                onPress={() => setShowScanner(!showScanner)}>
                <Text style={[Typography.base, Typography.accent]}>
                  {STRINGS.popcha_open_scanner}
                </Text>
              </PoPTouchableOpacity>
            </View>
          </View>
        </View>
      </QrCodeScanner>
    </>
  );
};

export default PopchaScanner;

export const PopchaScannerScreen: PopchaFeature.LaoScreen = {
  id: STRINGS.navigation_lao_popcha,
  Icon: makeIcon('key'),
  Component: PopchaScanner,
  order: 100000,
};
