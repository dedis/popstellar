import React, { useState } from 'react';
import {
  StyleSheet, View, TextInput, TextStyle, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';

import { dispatch, KeyPairStore, OpenedLaoStore } from 'store';
import { getNetworkManager } from 'network';

import {
  Hash, Lao, Timestamp,
} from 'model/objects';

import WideButtonView from 'components/WideButtonView';
import TextBlock from 'components/TextBlock';

import { Spacing, Typography } from 'styles';
import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import styleContainer from 'styles/stylesheets/container';

/**
 * Manage the Launch screen: a description string, a LAO name text input, a launch LAO button,
 * and cancel button
 *
 * The Launch button does nothing
 * The cancel button clear the LAO name field and redirect to the Home screen
 *
 * TODO implement the launch button action
 */
const styles = StyleSheet.create({
  textInput: {
    ...Typography.base,
    borderBottomWidth: 2,
    marginVertical: Spacing.s,
    marginHorizontal: Spacing.xl,
  } as TextStyle,
  viewTop: {
    justifyContent: 'flex-start',
  } as ViewStyle,
  viewBottom: {
    justifyContent: 'flex-end',
  } as ViewStyle,
});

const Launch = ({ navigation }: IPropTypes) => {
  const [inputLaoName, setInputLaoName] = useState('');

  const onButtonLaunchPress = (laoName: string) => {
    if (!laoName) {
      return;
    }
    // Navigate into launch confirm screen, and pass the lao name entered by the user.
    navigation.navigate(STRINGS.launch_navigation_tab_confirm, { laoName: inputLaoName });
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
    <View style={styleContainer.flex}>
      <View style={styles.viewTop}>
        <TextBlock text={STRINGS.launch_description} />
        <TextInput
          style={styles.textInput}
          placeholder={STRINGS.launch_organization_name}
          onChangeText={(input: string) => setInputLaoName(input)}
          defaultValue={inputLaoName}
        />
      </View>
      <View style={styles.viewBottom}>
        <WideButtonView
          title={`${STRINGS.launch_button_launch} -- Connect, Create LAO & Open UI`}
          onPress={onButtonLaunchPress}
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
