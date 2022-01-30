import React, { useState } from 'react';
import {
  StyleSheet, View, ViewStyle,
} from 'react-native';

import PropTypes from 'prop-types';

import { getNetworkManager } from 'network';
import { subscribeToChannel } from 'network/CommunicationApi';
import { Channel, channelFromIds, Hash } from 'model/objects';

import { Spacing } from 'styles';
import containerStyles from 'styles/stylesheets/containerStyles';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';

import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import TextInputLine from 'components/TextInputLine';
import { useToast } from 'react-native-toast-notifications';

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
const FOUR_SECONDS = 4000;

/**
 * Connects to the given server URL.
 *
 * @param serverUrl
 */
export function connectTo(serverUrl: string): boolean {
  try {
    const { href } = new URL(serverUrl); // validate
    getNetworkManager().connect(href);
  } catch (err) {
    console.error(`Cannot connect to '${serverUrl}' as it is an invalid URL`, err);
    return false;
  }
  return true;
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

const ConnectConfirm = ({ navigation, route }: IPropTypes) => {
  const [serverUrl, setServerUrl] = useState('wss://popdemo.dedis.ch/demo');
  const [laoId, setLaoId] = useState('');
  const toast = useToast();

  if (route.params && laoId === '') {
    setLaoId(route.params.laoIdIn);
    console.log(laoId);
  }

  const onButtonConfirm = async () => {
    if (!connectTo(serverUrl)) {
      return;
    }

    const channel = validateLaoId(laoId);
    if (channel === undefined) {
      return;
    }

    try {
      await subscribeToChannel(channel);
      navigation.navigate(STRINGS.app_navigation_tab_organizer, {
        screen: 'Attendee',
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
      <WideButtonView
        title={STRINGS.general_button_confirm}
        onPress={() => onButtonConfirm()}
      />
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
