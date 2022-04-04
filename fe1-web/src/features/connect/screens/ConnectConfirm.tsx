import { useNavigation, useRoute } from '@react-navigation/core';
import React, { useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';

import { TextBlock, TextInputLine, WideButtonView } from 'core/components';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { NetworkConnection } from 'core/network/NetworkConnection';
import { Spacing } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import STRINGS from 'resources/strings';

import { ConnectHooks } from '../hooks';

/**
 * Ask for confirmation to connect to a specific LAO
 * The ScrollView shows information for the user to verify the authenticity of the LAO
 *
 * TODO Make the confirm button make the action require in the UI specification
 */
const styles = StyleSheet.create({
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  } as ViewStyle,
});

/**
 * Connects to the given server URL.
 *
 * @param serverUrl
 */
export function connectTo(serverUrl: string): NetworkConnection | undefined {
  try {
    const { href } = new URL(serverUrl); // validate
    return getNetworkManager().connect(href);
  } catch (err) {
    console.error(`Cannot connect to '${serverUrl}' as it is an invalid URL`, err);
    return undefined;
  }
}

const ConnectConfirm = () => {
  // FIXME: route should use proper type
  const navigation = useNavigation<any>();
  const route = useRoute<any>();

  const laoIdIn = route.params?.laoIdIn || '';
  const url = route.params?.url || 'ws://localhost:9000/organizer/client';
  const [serverUrl, setServerUrl] = useState(url);
  const [laoId, setLaoId] = useState(laoIdIn);

  const toast = useToast();
  const getLaoChannel = ConnectHooks.useGetLaoChannel();

  const onButtonConfirm = async () => {
    const connection = connectTo(serverUrl);
    if (!connection) {
      return;
    }

    try {
      const channel = getLaoChannel(laoId);
      if (!channel) {
        throw new Error('The given LAO ID is invalid');
      }

      // subscribe to the lao channel on the new connection
      await subscribeToChannel(channel, [connection]);

      navigation.navigate(STRINGS.app_navigation_tab_lao, {
        screen: STRINGS.organization_navigation_tab_events,
      });
    } catch (err) {
      console.error(`Failed to establish lao connection: ${err}`);
      toast.show(`Failed to establish lao connection: ${err}`, {
        type: 'danger',
        placement: 'top',
        duration: FOUR_SECONDS,
      });
    }
  };

  return (
    <View style={containerStyles.flex}>
      <View style={styles.viewCenter}>
        <TextBlock text={STRINGS.connect_confirm_description} />
        <TextInputLine
          placeholder={STRINGS.connect_server_uri}
          onChangeText={(input: string) => setServerUrl(input)}
          defaultValue={serverUrl}
        />
        <TextInputLine
          placeholder={STRINGS.connect_lao_id}
          onChangeText={(input: string) => setLaoId(input)}
          defaultValue={laoId}
        />
      </View>
      <WideButtonView title={STRINGS.general_button_confirm} onPress={() => onButtonConfirm()} />
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={() => navigation.navigate(STRINGS.connect_unapproved_title)}
      />
    </View>
  );
};

export default ConnectConfirm;
