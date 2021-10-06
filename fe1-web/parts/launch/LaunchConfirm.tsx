import React, { useState } from 'react';
import {
  StyleSheet, TextInput, TextStyle, View, ViewStyle,
} from 'react-native';
import { subscribeToChannel } from '../../network/CommunicationApi';
import STRINGS from '../../res/strings';
import styleContainer from '../../styles/stylesheets/container';
import TextBlock from '../../components/TextBlock';
import WideButtonView from '../../components/WideButtonView';
import { connectTo, validateLaoId } from '../connect/ConnectConfirm';
import { Spacing, Typography } from '../../styles';

/**
 * UI to ask the address where you want to connect after having launched an LAO.
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

const LaunchConfirm = ({ navigation, route }: IPropTypes) => {
  const initialAddress = 'ws://127.0.0.1:9000/organizer/client';
  const [serverUrl, setServerUrl] = useState(initialAddress);
  const [laoId, setLaoId] = useState('');

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
    }
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
        onPress={() => navigation.navigate(STRINGS.connect_unapproved_title)}
      />
    </View>
  );
};
