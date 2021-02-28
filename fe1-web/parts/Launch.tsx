import React, { useState } from 'react';
import {
  StyleSheet, View, TextInput, TextStyle, ViewStyle,
} from 'react-native';
import { Spacing, Typography } from 'styles/index';

import * as RootNavigation from 'navigation/RootNavigation';
import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import { requestCreateLao } from 'network/MessageApi';
import { dispatch, KeyPairStore, OpenedLaoStore } from 'store';
import { getNetworkManager } from 'network';
import WideButtonView from 'components/WideButtonView';
import TextBlock from 'components/TextBlock';
import PropTypes from 'prop-types';
import styleContainer from 'styles/stylesheets/container';
import { Hash, Lao, Timestamp } from 'model/objects';

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
      requestCreateLao(laoName);
    } else {
      console.error('empty lao name');
    }
  };

  const onTestOpenConnection = () => {
    const nc = getNetworkManager().connect('127.0.0.1');
    nc.setRpcHandler(() => {
      console.info('Using custom test rpc handler : does nothing');
    });

    const org = KeyPairStore.getPublicKey();
    const time = new Timestamp(1609455600);
    const sampleLao: Lao = new Lao({
      name: 'name de la Lao',
      id: Hash.fromStringArray(org.toString(), time.toString(), 'name'),
      creation: time,
      last_modified: time,
      organizer: org,
      witnesses: [],
    });

    OpenedLaoStore.store(sampleLao);
    console.info('Stored test lao in storage : ', sampleLao);
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
