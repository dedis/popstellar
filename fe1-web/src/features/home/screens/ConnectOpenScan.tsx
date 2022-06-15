import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useState } from 'react';
import { StyleSheet, TouchableOpacity, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch } from 'react-redux';

import { PoPIcon } from 'core/components';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { Color, Icon, Spacing } from 'core/styles';
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

type NavigationProps = CompositeScreenProps<
  StackScreenProps<ConnectParamList, typeof STRINGS.navigation_connect_scan>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

/**
 * Starts a QR code scan
 */
const ConnectOpenScan = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const dispatch = useDispatch();

  const toast = useToast();

  const laoId = HomeHooks.useCurrentLaoId();
  const getLaoChannel = HomeHooks.useGetLaoChannel();
  const getLaoById = HomeHooks.useGetLaoById();

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

      console.info(
        `Trying to connect to lao with id '${connectToLao.lao}' on '${connectToLao.servers}'.`,
      );

      // connect to the lao
      const connections = connectToLao.servers.map((server) => getNetworkManager().connect(server));
      if (connections.length === 0) {
        return;
      }

      const laoChannel = getLaoChannel(connectToLao.lao);
      if (!laoChannel) {
        // invalid lao id
        toast.show(STRINGS.connect_scanning_fail, {
          type: 'warning',
          placement: 'top',
          duration: FOUR_SECONDS,
        });
        return;
      }

      const lao = getLaoById(connectToLao.lao);
      let channels = [laoChannel];

      if (lao) {
        channels = lao.subscribed_channels;
      }

      // subscribe to the lao channel (or all previously subscribed to channels) on the new connection
      Promise.all(
        channels.map((channel) =>
          subscribeToChannel(connectToLao.lao, dispatch, channel, connections),
        ),
      ).then(() => {
        navigation.navigate(STRINGS.navigation_app_lao, {
          screen: STRINGS.navigation_lao_home,
        });
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
              <PoPIcon name="close" color={Color.accent} size={Icon.size} />
            </TouchableOpacity>
          </View>
        </View>
        <View>
          <View style={styles.rightButtons}>
            <TouchableOpacity
              style={styles.buttonMargin}
              onPress={() => navigation.navigate(STRINGS.navigation_connect_launch)}>
              <PoPIcon name="create" color={Color.accent} size={Icon.size} />
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => navigation.navigate(STRINGS.navigation_connect_confirm)}>
              <PoPIcon name="code" color={Color.accent} size={Icon.size} />
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </QrCodeScanner>
  );
};

export default ConnectOpenScan;
