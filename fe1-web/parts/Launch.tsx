import React, { useState } from 'react';
import {
  StyleSheet, View, TextInput, TextStyle, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';

import { dispatch, KeyPairStore, OpenedLaoStore } from 'store';
import { getNetworkManager, requestCreateLao } from 'network';
import { JsonRpcRequest } from 'model/network';
import { Hash, Lao, Timestamp } from 'model/objects';

import { Spacing, Typography } from 'styles';
import styleContainer from 'styles/stylesheets/container';
import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';

import * as RootNavigation from 'navigation/RootNavigation';
import WideButtonView from 'components/WideButtonView';
import TextBlock from 'components/TextBlock';

// temporarily used for testing
import testKeyPair from 'test_data/keypair.json';

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
    if (laoName) {
      requestCreateLao(laoName)
        .then(() => {
          console.info('LAO created successfully');
        })
        .catch((err) => {
          console.error('Could not create LAO:', err);
        });
    } else {
      console.error('Could not create LAO without a name');
    }
  };

  const onTestOpenConnection = () => {
    const org = KeyPairStore.getPublicKey();
    const time = new Timestamp(1609455600);
    const sampleLao: Lao = new Lao({
      name: 'name',
      id: Hash.fromStringArray(org.toString(), time.toString(), 'name'),
      creation: time,
      last_modified: time,
      organizer: org,
      witnesses: [],
    });

    // bad practice to access the store directly, but OK for testing
    OpenedLaoStore.store(sampleLao);

    getNetworkManager().setRpcHandler((m: JsonRpcRequest) => {
      console.info('Handling the json-rpc response : ', m);
    });
    getNetworkManager().connect('127.0.0.1');
  };

  const onTestClearStorage = () => {
    dispatch({ type: 'CLEAR_STORAGE', value: {} });
  };

  const cancelAction = () => {
    setInputLaoName('');
    navigation.navigate('Home');
  };

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
          title={STRINGS.launch_button_launch}
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
        <WideButtonView
          title="[TEST] GoTo newly created LAO"
          onPress={() => RootNavigation.navigate(STRINGS.app_navigation_tab_organizer, {})}
        />
        <WideButtonView
          title={STRINGS.general_button_cancel}
          onPress={cancelAction}
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
