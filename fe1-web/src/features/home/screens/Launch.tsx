import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';
import { useDispatch } from 'react-redux';

import { Input, PoPTextButton } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { Channel, getLaoIdFromChannel } from 'core/objects';
import { Typography } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import STRINGS from 'resources/strings';

import { HomeHooks } from '../hooks';

/**
 * Manages the Launch screen, where the user enters a name and an address to launch and connect
 * to an LAO.
 */

const styles = StyleSheet.create({
  viewTop: {
    justifyContent: 'flex-start',
  } as ViewStyle,
  viewBottom: {
    justifyContent: 'flex-end',
  } as ViewStyle,
});

type NavigationProps = CompositeScreenProps<
  StackScreenProps<ConnectParamList, typeof STRINGS.navigation_connect_launch>,
  StackScreenProps<AppParamList, typeof STRINGS.navigation_app_home>
>;

const Launch = () => {
  const navigation = useNavigation<NavigationProps['navigation']>();
  const dispatch = useDispatch();

  const [inputLaoName, setInputLaoName] = useState('');
  const [inputAddress, setInputAddress] = useState('ws://127.0.0.1:9000/organizer/client');

  const connectToTestLao = HomeHooks.useConnectToTestLao();
  const requestCreateLao = HomeHooks.useRequestCreateLao();

  const onButtonLaunchPress = async (laoName: string) => {
    if (!laoName) {
      return;
    }

    try {
      // establish connection
      await getNetworkManager().connect(inputAddress);

      // create lao
      const channel: Channel = await requestCreateLao(laoName);
      const laoId = getLaoIdFromChannel(channel);

      // subscribe to the just created lao channel
      await subscribeToChannel(laoId, dispatch, channel);

      // navigate to the newly created LAO
      navigation.navigate(STRINGS.navigation_app_lao, {
        screen: STRINGS.navigation_lao_home,
      });
    } catch (e) {
      console.error(`Failed to establish lao connection`, e);
    }
  };

  return (
    <ScreenWrapper>
      <View style={containerStyles.flex}>
        <View style={styles.viewTop}>
          <Text style={Typography.heading}>{STRINGS.launch_heading}</Text>

          <Text style={[Typography.paragraph, Typography.important]}>
            {STRINGS.launch_organization_name}
          </Text>
          <Input
            value={inputLaoName}
            onChange={setInputLaoName}
            placeholder={STRINGS.launch_organization_name}
            testID="launch_organization_name_selector"
          />

          <Text style={[Typography.paragraph, Typography.important]}>{STRINGS.launch_address}</Text>
          <Input
            value={inputAddress}
            onChange={setInputAddress}
            placeholder={STRINGS.launch_address}
            testID="launch_address_selector"
          />
        </View>
        <View style={styles.viewBottom}>
          <PoPTextButton
            onPress={() => onButtonLaunchPress(inputLaoName)}
            testID="launch_launch_selector">
            {STRINGS.launch_button_launch}
          </PoPTextButton>

          <PoPTextButton onPress={connectToTestLao}>
            [TEST] Connect to LocalMockServer.ts (use &apos;npm run startServer&apos;)
          </PoPTextButton>
        </View>
      </View>
    </ScreenWrapper>
  );
};

export default Launch;
