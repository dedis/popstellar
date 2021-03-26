import React, { useState } from 'react';
import {
  StyleSheet, View, ViewStyle, TextInput, TextStyle,
} from 'react-native';
import PropTypes from 'prop-types';

import { getNetworkManager, requestCreateLao } from 'network';
import { establishLaoConnection } from 'network/CommunicationApi';
import { Channel, channelFromId, Hash } from 'model/objects';

import { Spacing, Typography } from 'styles';
import styleContainer from 'styles/stylesheets/container';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';

import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';

/**
 * Ask for confirmation to connect to a specific LAO
 * The ScrollView shows information for the user to verify the authenticity of the LAO
 *
 * TODO Make the confirm button make the action require in the UI specification
*/
const styles = StyleSheet.create({
  textInput: {
    ...Typography.base,
    borderBottomWidth: 2,
    marginVertical: Spacing.s,
    marginHorizontal: Spacing.xl,
  } as TextStyle,
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  } as ViewStyle,
});

function connectTo(serverUrl: string): boolean {
  try {
    const { hostname, port, pathname } = new URL(serverUrl);

    const portNum = port ? parseInt(port, 10) : undefined;
    const path = pathname.replace(/^\/+/g, '');

    getNetworkManager().connect(hostname, portNum, path || undefined);
  } catch (err) {
    console.error(`Cannot connect to '${serverUrl}' as it is an invalid URL`, err);
    return false;
  }
  return true;
}

function validateLaoId(laoId: string): Channel | undefined {
  try {
    const h = new Hash(laoId);
    return channelFromId(h);
  } catch (err) {
    console.error(`Cannot connect to LAO '${laoId}' as it is an invalid LAO ID`, err);
  }
  return undefined;
}

const ConnectConfirm = ({ navigation }: IPropTypes) => {
  const [serverUrl, setServerUrl] = useState('https://127.0.0.1:8080');
  const [laoId, setLaoId] = useState('');

  const onButtonConfirm = () => {
    const parentNavigation = navigation.dangerouslyGetParent();
    if (parentNavigation === undefined) {
      return;
    }

    if (!connectTo(serverUrl)) {
      return;
    }

    const channel = validateLaoId(laoId);
    if (channel === undefined) {
      return;
    }

    establishLaoConnection(channel)
      .then(() => {
        // navigate to the newly created LAO
        navigation.navigate(STRINGS.organization_navigation_tab_attendee, {});
      })
      .catch((reason) => console.error(`Failed to establish lao connection: ${reason}`));
  };

  return (
    <View style={styleContainer.flex}>
      <View style={styles.viewCenter}>
        <TextBlock text={STRINGS.connect_confirm_description} />
        <TextInput
          style={styles.textInput}
          placeholder={STRINGS.connect_server_uri}
          onChangeText={(input: string) => setServerUrl(input)}
          defaultValue={serverUrl}
        />
        <TextInput
          style={styles.textInput}
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
        onPress={() => navigation.navigate(STRINGS.connect_scanning_title)}
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
