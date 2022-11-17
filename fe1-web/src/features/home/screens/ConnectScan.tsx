import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch } from 'react-redux';

import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import QrCodeScanOverlay from 'core/components/QrCodeScanOverlay';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { Channel } from 'core/objects';
import { Spacing, Typography } from 'core/styles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';
import { ConnectToLao } from '../objects';

type NavigationProps = CompositeScreenProps<
  StackScreenProps<ConnectParamList, typeof STRINGS.navigation_connect_scan>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'space-between',
    marginVertical: Spacing.contentSpacing,
  } as ViewStyle,
  qrCode: {
    opacity: 0.5,
  } as ViewStyle,
  enterManually: {} as ViewStyle,
});

/**
 * Starts a QR code scan
 */
const ConnectScan = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const dispatch = useDispatch();

  const toast = useToast();

  const laoId = HomeHooks.useCurrentLaoId();
  const getLaoChannel = HomeHooks.useGetLaoChannel();
  const getLaoById = HomeHooks.useGetLaoById();
  const resubscribeToLao = HomeHooks.useResubscribeToLao();

  // this is needed as otherwise the camera may stay turned on
  const [showScanner, setShowScanner] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const isProcessingScan = useRef(false);

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

  /**
   * Checks if the passed connection data is valid
   * @param data A stringified ConnectToLao object
   */
  const validateConnectionData = (
    data: string,
  ): { connectToLao: ConnectToLao; laoChannel: Channel } | false => {
    const obj = JSON.parse(data);

    try {
      const connectToLao = ConnectToLao.fromJson(obj);

      if (connectToLao.servers.length === 0) {
        throw new Error('The scanned QR code did not contain any server address');
      }

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

        return false;
      }

      const laoChannel = getLaoChannel(connectToLao.lao);

      if (!laoChannel) {
        // invalid lao id
        toast.show(STRINGS.connect_scanning_fail, {
          type: 'warning',
          placement: 'top',
          duration: FOUR_SECONDS,
        });

        return false;
      }

      return { connectToLao, laoChannel };
    } catch (e) {
      console.error(e);

      toast.show(`The scanned QR code is invalid`, {
        type: 'warning',
        placement: 'top',
        duration: FOUR_SECONDS,
      });

      return false;
    }
  };

  const connectToLaoAndSubscribe = async (connectToLao: ConnectToLao, laoChannel: Channel) => {
    // connect to the lao
    const connections = await Promise.all(
      connectToLao.servers.map((server) => getNetworkManager().connect(server)),
    );
    if (connections.length !== connectToLao.servers.length) {
      throw new Error(
        `networkManager.connect() should either return a connection or throw an error.
        ${connectToLao.servers.length} addresses were passed and ${connections.length} were received`,
      );
    }

    const lao = getLaoById(connectToLao.lao);

    if (lao) {
      // subscribe to all previously subscribed to channels on the new connectionss
      return resubscribeToLao(lao, dispatch, connections);
    }

    // subscribe to the lao channel on the new connections
    return subscribeToChannel(connectToLao.lao, dispatch, laoChannel, connections);
  };

  const handleScan = async (data: string | null) => {
    if (!data || isProcessingScan.current) {
      return;
    }

    const validatedConnectionData = validateConnectionData(data);
    if (!validatedConnectionData) {
      return;
    }

    const { connectToLao, laoChannel } = validatedConnectionData;

    try {
      // prevent the function from being executed twice
      // it is okay to only call this here (after the above checks) since
      // javascript is single threaded and will not stop executing this function
      // in the middle
      isProcessingScan.current = true;
      // update (and possibly disable) the UI while the scanned data is being processed
      setIsConnecting(true);

      console.info(
        `Trying to connect to lao with id '${connectToLao.lao}' on '${connectToLao.servers}'.`,
      );

      await connectToLaoAndSubscribe(connectToLao, laoChannel);

      isProcessingScan.current = false;
      setIsConnecting(false);

      navigation.navigate(STRINGS.navigation_app_lao, {
        screen: STRINGS.navigation_lao_events,
        params: { screen: STRINGS.navigation_lao_events_home },
      });
    } catch (error) {
      // close already established connections
      getNetworkManager().disconnectFromAll();

      // allow scanning of new codes
      isProcessingScan.current = false;
      setIsConnecting(false);

      toast.show(STRINGS.connect_scanning_fail, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  if (isConnecting) {
    return (
      <QrCodeScanner showCamera={showScanner} handleScan={handleScan}>
        <Text style={Typography.base}>{STRINGS.navigation_connect_processing}</Text>
      </QrCodeScanner>
    );
  }

  return (
    <QrCodeScanner showCamera={showScanner} handleScan={handleScan}>
      <View style={styles.container}>
        <View />
        <View style={styles.qrCode}>
          <QrCodeScanOverlay width={300} height={300} />
        </View>
        <View style={styles.enterManually}>
          <View style={QrCodeScannerUIElementContainer}>
            <PoPTouchableOpacity
              onPress={() => navigation.push(STRINGS.navigation_connect_confirm)}>
              <Text style={[Typography.base, Typography.accent]}>
                {STRINGS.general_enter_manually}
              </Text>
            </PoPTouchableOpacity>
          </View>
        </View>
      </View>
    </QrCodeScanner>
  );
};

export default ConnectScan;
