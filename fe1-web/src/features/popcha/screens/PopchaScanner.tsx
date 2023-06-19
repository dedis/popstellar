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

import { sendAuthRequest } from '../functions/network';
import { PopchaHooks } from '../hooks';
import { PopchaFeature } from '../interface';

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

  return showScanner ? (
    <>
      <QrCodeScanner
        showCamera
        handleScan={(data: string | null) =>
          data &&
          sendAuthRequest(data, laoId, generateToken)
            .then(() => {
              setShowScanner(false);
              showSuccessMessage(STRINGS.popcha_success_authentication);
            })
            .catch((error) => {
              showErrorMessage(error.toString());
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
          sendAuthRequest(text, laoId, generateToken)
            .then(() => {
              setShowScanner(false);
              setInputModalIsVisible(false);
              showSuccessMessage(STRINGS.popcha_success_authentication);
            })
            .catch((error) => {
              showErrorMessage(error.toString());
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
