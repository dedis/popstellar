import { useRoute } from '@react-navigation/core';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import { StyleSheet, View, ViewStyle } from 'react-native';
import { useToast } from 'react-native-toast-notifications';
import { useDispatch } from 'react-redux';

import { TextBlock, TextInputLine, WideButtonView } from 'core/components';
import { getNetworkManager, subscribeToChannel } from 'core/network';
import { NetworkConnection } from 'core/network/NetworkConnection';
import { Channel, channelFromIds, Hash } from 'core/objects';
import { Spacing } from 'core/styles';
import containerStyles from 'core/styles/stylesheets/containerStyles';
import { FOUR_SECONDS } from 'resources/const';
import PROPS_TYPE from 'resources/Props';
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

/**
 * Checks if the LAO exists by trying to find its id in created channels.
 *
 * @param laoId the id of the LAO we want to validate
 */
export function validateLaoId(laoId: string): Channel | undefined {
  try {
    const h = new Hash(laoId);
    return channelFromIds(h);
  } catch (err) {
    console.error(`Cannot connect to LAO '${laoId}' as it is an invalid LAO ID`, err);
  }
  return undefined;
}

const ConnectConfirm = ({ navigation }: IPropTypes) => {
  // FIXME: route should use proper type
  const route = useRoute<any>();
  const laoIdIn = route.params?.laoIdIn || '';
  const url = route.params?.url || 'ws://localhost:9000/organizer/client';
  const [serverUrl, setServerUrl] = useState(url);
  const [laoId, setLaoId] = useState(laoIdIn);

  const toast = useToast();
  const dispatch = useDispatch();
  const addLaoServerAddress = ConnectHooks.useAddLaoServerAddress();

  const onButtonConfirm = async () => {
    const connection = connectTo(serverUrl);
    if (!connection) {
      return;
    }

    const channel = validateLaoId(laoId);
    if (channel === undefined) {
      return;
    }

    try {
      // subscribe to the lao channel on the new connection
      await subscribeToChannel(channel, [connection]);
      // add the new server address to the store
      dispatch(addLaoServerAddress(laoId, serverUrl));

      navigation.navigate(STRINGS.app_navigation_tab_organizer, {
        screen: STRINGS.organization_navigation_tab_organizer,
        params: {
          screen: STRINGS.organizer_navigation_tab_home,
          params: { url: serverUrl },
        },
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

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

ConnectConfirm.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default ConnectConfirm;
