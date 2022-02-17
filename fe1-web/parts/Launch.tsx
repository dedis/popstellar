import React, { useState } from 'react';
import {
  StyleSheet, View, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';

import { dispatch, KeyPairStore, OpenedLaoStore } from 'store';
import { getNetworkManager, requestCreateLao } from 'network';

import {
  Channel, Hash, Lao, Timestamp,
} from 'model/objects';

import { TextBlock, TextInputLine, WideButtonView } from 'core/components';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import containerStyles from 'styles/stylesheets/containerStyles';
import { subscribeToChannel } from 'network/CommunicationApi';

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

const Launch = ({ navigation }: IPropTypes) => {
  const [inputLaoName, setInputLaoName] = useState('');
  const [inputAddress, setInputAddress] = useState('ws://127.0.0.1:9000/organizer/client');

  const onButtonLaunchPress = (laoName: string) => {
    if (!laoName) {
      return;
    }

    getNetworkManager().connect(inputAddress);
    requestCreateLao(laoName)
      .then((channel: Channel) => subscribeToChannel(channel)
        .then(() => {
          // navigate to the newly created LAO
          navigation.navigate(STRINGS.app_navigation_tab_organizer, {
            screen: STRINGS.organization_navigation_tab_organizer,
            params: {
              screen: STRINGS.organizer_navigation_tab_home,
              params: { url: inputAddress },
            },
          });
        }))
      .catch(
        ((reason) => console.debug(`Failed to establish lao connection: ${reason}`)),
      );
  };

  const onTestOpenConnection = () => {
    const nc = getNetworkManager().connect('ws://127.0.0.1:9000/organizer/client');
    nc.setRpcHandler(() => {
      console.info('Using custom test rpc handler: does nothing');
    });

    const org = KeyPairStore.getPublicKey();
    const time = new Timestamp(1609455600);
    const sampleLao: Lao = new Lao({
      name: 'name de la Lao',
      id: new Hash('myLaoId'), // Hash.fromStringArray(org.toString(), time.toString(), 'name')
      creation: time,
      last_modified: time,
      organizer: org,
      witnesses: [],
    });

    OpenedLaoStore.store(sampleLao);
    console.info('Stored test lao in storage : ', sampleLao);
  };

  const onTestClearStorage = () => dispatch({ type: 'CLEAR_STORAGE', value: {} });

  return (
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
        <WideButtonView
          title={`${STRINGS.launch_button_launch} -- Connect, Create LAO & Open UI`}
          onPress={() => onButtonLaunchPress(inputLaoName)}
        />
        <WideButtonView
          title="[TEST] Connect to LocalMockServer.ts (use 'npm run startServer')"
          onPress={onTestOpenConnection}
        />
        <WideButtonView
          title="[TEST] Clear (persistent) storage"
          onPress={onTestClearStorage}
        />
      </View>
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};
Launch.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default Launch;
