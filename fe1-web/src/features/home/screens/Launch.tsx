import { CompositeScreenProps, useNavigation } from '@react-navigation/core';
import { StackScreenProps } from '@react-navigation/stack';
import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { Input, PoPTextButton } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
import { AppParamList } from 'core/navigation/typing/AppParamList';
import { ConnectParamList } from 'core/navigation/typing/ConnectParamList';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { Channel } from 'core/objects';
import { dispatch } from 'core/redux';
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

  const [inputLaoName, setInputLaoName] = useState('');
  const [inputAddress, setInputAddress] = useState('ws://127.0.0.1:9000/organizer/client');

  const connectToTestLao = HomeHooks.useConnectToTestLao();
  const requestCreateLao = HomeHooks.useRequestCreateLao();

  const onButtonLaunchPress = (laoName: string) => {
    if (!laoName) {
      return;
    }

    getNetworkManager().connect(inputAddress);

    requestCreateLao(laoName)
      .then((channel: Channel) =>
        subscribeToChannel(channel).then(() => {
          // navigate to the newly created LAO
          navigation.navigate(STRINGS.navigation_app_lao, {
            screen: STRINGS.navigation_lao_home,
          });
        }),
      )
      .catch((reason) => console.debug(`Failed to establish lao connection: ${reason}`));
  };

  const onTestClearStorage = () => dispatch({ type: 'CLEAR_STORAGE', value: {} });

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

          <PoPTextButton onPress={onTestClearStorage}>
            [TEST] Clear (persistent) storage
          </PoPTextButton>

          <PoPTextButton onPress={navigation.goBack}>{STRINGS.general_button_cancel}</PoPTextButton>
        </View>
      </View>
    </ScreenWrapper>
  );
};

export default Launch;
