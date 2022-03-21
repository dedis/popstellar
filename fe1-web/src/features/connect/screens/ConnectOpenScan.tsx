import { useNavigation } from '@react-navigation/core';
import React, { useEffect, useState } from 'react';
import { View } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import QrReader from 'react-qr-reader';

import { WideButtonView } from 'core/components';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { ConnectHooks } from '../hooks';
import { ConnectToLao } from '../objects';

/**
 * Starts a QR code scan
 */
const ConnectOpenScan = () => {
  // FIXME: route should use proper type
  const navigation = useNavigation<any>();

  // Remove the user to go back to the ConnectEnableCamera as he has already given
  // his permission to use the camera

  // this is needed as otherwise the camera will stay turned on
  const [showScanner, setShowScanner] = useState(false);
  // re-enable scanner on focus events
  useEffect(() => {
    const unsubscribe = navigation.addListener('focus', () => {
      // The screen is focused, set QrWasScanned to false (i.e. allow scanner to be reused)
      setShowScanner(true);
    });

    // Return the function to unsubscribe from the event so it gets removed on unmount
    return unsubscribe;
  }, [navigation]);

  const toast = useToast();

  const laoId = ConnectHooks.useCurrentLaoId();

  const handleError = (err: string) => {
    console.error(err);
    toast.show(err, {
      type: 'danger',
      placement: 'top',
      duration: FOUR_SECONDS,
    });
  };

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

  return showScanner ? (
    <View style={containerStyles.centeredXY}>
      <QrReader delay={300} onError={handleError} onScan={handleScan} style={{ width: '30%' }} />
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={() => {
          setShowScanner(false);

          // if we have an active lao, this was an additional connection and thus we navigate (back)
          // to the organization user screen
          if (laoId) {
            navigation.navigate(STRINGS.app_navigation_tab_user, {
              screen: STRINGS.organization_navigation_tab_user,
            });
          } else {
            navigation.navigate(STRINGS.connect_unapproved_title);
          }
        }}
      />
    </View>
  ) : (
    <View style={containerStyles.centeredY} />
  );
};

export default ConnectOpenScan;
