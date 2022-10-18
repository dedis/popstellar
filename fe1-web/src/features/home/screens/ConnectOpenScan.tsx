import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useEffect, useRef, useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch } from 'react-redux';

import { PoPIcon } from 'core/components';
import PoPTouchableOpacity from 'core/components/PoPTouchableOpacity';
import QrCodeScanner, { QrCodeScannerUIElementContainer } from 'core/components/QrCodeScanner';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { Channel } from 'core/objects';
import { Color, Icon, Spacing, Typography } from 'core/styles';
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
        screen: STRINGS.navigation_lao_home,
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
      <View style={styles.buttonContainer}>
        <View>
          <View style={styles.leftButtons}>
            <PoPTouchableOpacity onPress={() => navigation.goBack()}>
              <PoPIcon name="close" color={Color.accent} size={Icon.size} />
            </PoPTouchableOpacity>
          </View>
        </View>
        <View>
          <View style={styles.rightButtons}>
            <PoPTouchableOpacity
              style={styles.buttonMargin}
              onPress={() => navigation.navigate(STRINGS.navigation_connect_launch)}
              testID="launch_selector">
              <PoPIcon name="create" color={Color.accent} size={Icon.size} />
            </PoPTouchableOpacity>
            <PoPTouchableOpacity
              onPress={() => navigation.navigate(STRINGS.navigation_connect_confirm)}>
              <PoPIcon name="code" color={Color.accent} size={Icon.size} />
            </PoPTouchableOpacity>
          </View>
        </View>
      </View>
    </QrCodeScanner>
  );
};

export default ConnectOpenScan;
