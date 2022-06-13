import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { TouchableOpacity } from 'react-native-gesture-handler';
import { useToast } from 'react-native-toast-notifications';

import { PoPIcon } from 'core/components';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { WalletParamList } from 'core/navigation/typing/WalletParamList';
import { Color, Icon } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { DigitalCashFeature } from '../interface';

/**
 * UI for a currently opened roll call. From there, the organizer can scan attendees or add them
 * manually. At the end, he can close it by pressing on the close button.
 */

const styles = StyleSheet.create({
  buttonContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  leftButtons: QrCodeScannerUIElementContainer,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<WalletParamList, typeof STRINGS.navigation_wallet_digital_cash_wallet_scanner>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const tokenMatcher = new RegExp('^[A-Za-z0-9_-]{43}=$');

const PoPTokenScanner = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();

  // these parameters are required to navigate back to the digital cash wallet
  const { laoId, rollCallId, isCoinbase } = route.params;

  const toast = useToast();

  // this is needed as otherwise the camera may stay turned on
  const [showScanner, setShowScanner] = useState(false);

  // re-enable scanner on focus events
  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('focus', () => {
      // The screen is now focused, set showScanner to true
      setShowScanner(true);
    });
  }, [navigation]);

  // disable scanner on blur events
  useEffect(() => {
    // Return the function to unsubscribe from the event so it gets removed on unmount
    return navigation.addListener('blur', () => {
      // The screen is no longer focused, set showScanner to false (i.e. allow scanner to be reused)
      setShowScanner(false);
    });
  }, [navigation]);

  const goBack = (popToken?: string) => {
    navigation.navigate(STRINGS.navigation_wallet_digital_cash_send_receive, {
      laoId,
      rollCallId,
      isCoinbase,
      scannedPoPToken: popToken,
    });
  };

  const onScanData = (popToken: string | null) => {
    if (popToken) {
      if (tokenMatcher.test(popToken)) {
        goBack(popToken);
      } else {
        toast.show(STRINGS.roll_call_invalid_token, {
          type: 'danger',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
      }
    }
  };

  return (
    <>
      <QrCodeScanner showCamera={showScanner} handleScan={onScanData}>
        <View style={styles.buttonContainer}>
          <View>
            <View style={styles.leftButtons}>
              <TouchableOpacity onPress={() => goBack()}>
                <PoPIcon name="close" color={Color.accent} size={Icon.size} />
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </QrCodeScanner>
    </>
  );
};

export default PoPTokenScanner;

export const PoPTokenScannerScreen: DigitalCashFeature.WalletScreen = {
  id: STRINGS.navigation_wallet_digital_cash_wallet_scanner,
  title: STRINGS.digital_cash_wallet_screen_title,
  Component: PoPTokenScanner,
  headerShown: false,
};
