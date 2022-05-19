import { useNavigation } from '@react-navigation/core';
import React, { useState } from 'react';
import { StyleSheet, Text, View, ViewStyle } from 'react-native';

import { TextBlock, TextInputLine, Button } from 'core/components';
import ScreenWrapper from 'core/components/ScreenWrapper';
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

const Launch = () => {
  // FIXME: use proper navigation type
  const navigation = useNavigation<any>();

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
          navigation.navigate(STRINGS.navigation_app_tab_lao, {
            screen: STRINGS.organization_navigation_tab_events,
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
          <TextBlock text={STRINGS.launch_description} />
          <TextInputLine
            placeholder={STRINGS.launch_organization_name}
            onChangeText={(input: string) => setInputLaoName(input)}
            defaultValue={inputLaoName}
          />
          <TextInputLine
            placeholder={STRINGS.launch_address}
            onChangeText={(input: string) => setInputAddress(input)}
            defaultValue={inputAddress}
          />
        </View>
        <View style={styles.viewBottom}>
          <Button onPress={() => onButtonLaunchPress(inputLaoName)}>
            <Text
              style={[
                Typography.base,
                Typography.centered,
                Typography.negative,
              ]}>{`${STRINGS.launch_button_launch} -- Connect, Create LAO & Open UI`}</Text>
          </Button>

          <Button onPress={connectToTestLao}>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              [TEST] Connect to LocalMockServer.ts (use &apos;npm run startServer&apos;)
            </Text>
          </Button>

          <Button onPress={onTestClearStorage}>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>
              [TEST] Clear (persistent) storage
            </Text>
          </Button>

          <Button onPress={navigation.goBack}>
            <Text style={[Typography.base, Typography.centered, Typography.negative]}>Cancel</Text>
          </Button>
        </View>
      </View>
    </ScreenWrapper>
  );
};

export default Launch;
