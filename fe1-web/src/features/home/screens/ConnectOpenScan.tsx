import { useNavigation } from '@react-navigation/core';
import React, { useEffect, useState } from 'react';
import { StyleSheet, TouchableOpacity, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import CloseIcon from 'core/components/icons/CloseIcon';
import CodeIcon from 'core/components/icons/CodeIcon';
import CreateIcon from 'core/components/icons/CreateIcon';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { Border, Colors, Spacing } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { ConnectToLao } from '../objects';

const styles = StyleSheet.create({
  buttonContainer: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'space-between',
  } as ViewStyle,
  leftButtons: QrCodeScannerUIElementContainer,
  rightButtons: QrCodeScannerUIElementContainer,
  buttonMargin: {
    marginBottom: Spacing.x05,
  } as ViewStyle,
});

/**
 * Starts a QR code scan
 */
const ConnectOpenScan = () => {
  // FIXME: route should use proper type
  const navigation = useNavigation<any>();

  const toast = useToast();

  const laoId = HomeHooks.useCurrentLaoId();

  // this is needed as otherwise the camera will stay turned on
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

  const handleScan = (data: string | null) => {
    if (!data) {
      return;
    }

    try {
      const obj = JSON.parse(data);
      const connectToLao = ConnectToLao.fromJson(obj);

      // if we are already connected to a LAO, then only allow new connections
      // to servers for the same LAO id
      if (laoId && connectToLao.lao !== laoId.valueOf()) {
        toast.show(
          `The scanned QR code is for a different LAO than the one currently connected to`,
          {
            type: 'warning',
            placement: 'top',
            duration: FOUR_SECONDS,
          },
        );
        return;
      }

      setShowScanner(false);
      navigation.navigate(STRINGS.connect_confirm_title, {
        laoIdIn: connectToLao.lao,
        url: connectToLao.server,
      });
    } catch (error) {
      toast.show(STRINGS.connect_scanning_fail, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  return (
    <QrCodeScanner showCamera={showScanner} handleScan={handleScan}>
      <View style={styles.buttonContainer}>
        <View>
          <View style={styles.leftButtons}>
            <TouchableOpacity onPress={() => navigation.goBack()}>
              <CloseIcon color={Colors.primary} size={25} />
            </TouchableOpacity>
          </View>
        </View>
        <View>
          <View style={styles.rightButtons}>
            <TouchableOpacity
              style={styles.buttonMargin}
              onPress={() => navigation.navigate(STRINGS.navigation_tab_launch)}>
              <CreateIcon color={Colors.primary} size={25} />
            </TouchableOpacity>
            <TouchableOpacity onPress={() => navigation.navigate(STRINGS.connect_confirm_title)}>
              <CodeIcon color={Colors.primary} size={25} />
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </QrCodeScanner>
  );
};

export default ConnectOpenScan;
