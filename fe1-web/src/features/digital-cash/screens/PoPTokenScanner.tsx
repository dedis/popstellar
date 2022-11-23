import { CompositeScreenProps, useNavigation, useRoute } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { TouchableOpacity } from 'react-native-gesture-handler';
import { useToast } from 'react-native-toast-notifications';

import { PoPIcon } from 'core/components';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { DigitalCashParamList } from 'core/navigation/typing/DigitalCashParamList';
import { ScannablePopToken } from 'core/objects/ScannablePopToken';
import { Color, Icon } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

const styles = StyleSheet.create({
  buttonContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  leftButtons: QrCodeScannerUIElementContainer,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<DigitalCashParamList, typeof STRINGS.navigation_digital_cash_wallet_scanner>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_lao>
>;

const PoPTokenScanner = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const route = useRoute<NavigationProps['route']>();

  // these parameters are required to navigate back to the digital cash wallet
  const { rollCallId, isCoinbase } = route.params;

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
    navigation.navigate(STRINGS.navigation_digital_cash_send_receive, {
      rollCallId,
      isCoinbase,
      scannedPoPToken: popToken,
    });
  };

  const onScanData = (popToken: string | null) => {
    if (popToken) {
      try {
        const token = ScannablePopToken.fromJson(JSON.parse(popToken));
        goBack(token.pop_token);
      } catch {
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
