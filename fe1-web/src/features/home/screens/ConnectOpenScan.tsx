import { useNavigation } from '@react-navigation/core';
import { BarCodeScanningResult } from 'expo-camera';
import React, { useEffect, useState } from 'react';
import { StyleSheet, TouchableOpacity, View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { WideButtonView } from 'core/components';
import Camera from 'core/components/Camera';
import CloseIcon from 'core/components/icons/CloseIcon';
import CreateIcon from 'core/components/icons/CreateIcon';
import SettingsIcon from 'core/components/icons/SettingsIcon';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { Colors } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { ConnectToLao } from '../objects';

const styles = StyleSheet.create({
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  manualConnection: {
    flexDirection: 'row',
    justifyContent: 'flex-end',
  },
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
    <Camera showCamera={showScanner} handleScan={handleScan}>
      <View style={styles.buttonContainer}>
        <TouchableOpacity onPress={() => navigation.goBack()}>
          <CloseIcon color={Colors.primary} size={25} />
        </TouchableOpacity>
        <TouchableOpacity onPress={() => navigation.navigate(STRINGS.navigation_tab_launch)}>
          <CreateIcon color={Colors.primary} size={25} />
        </TouchableOpacity>
      </View>
      <View style={styles.manualConnection}>
        <TouchableOpacity onPress={() => navigation.navigate(STRINGS.connect_confirm_title)}>
          <SettingsIcon color={Colors.primary} size={25} />
        </TouchableOpacity>
      </View>
    </Camera>
  );
};

export default ConnectOpenScan;
