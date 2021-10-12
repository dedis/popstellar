import React, { useState } from 'react';
import {
  StyleSheet, TextInput, TextStyle, View, ViewStyle,
} from 'react-native';
import PropTypes from 'prop-types';
import { subscribeToChannel } from '../../network/CommunicationApi';
import STRINGS from '../../res/strings';
import styleContainer from '../../styles/stylesheets/container';
import TextBlock from '../../components/TextBlock';
import WideButtonView from '../../components/WideButtonView';
import { Spacing, Typography } from '../../styles';
import { getNetworkManager, requestCreateLao } from '../../network';
import { Channel } from '../../model/objects';
import PROPS_TYPE from '../../res/Props';

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
  const { laoName } = route.params;

  const onButtonConfirm = async () => {
    getNetworkManager().connect(serverUrl);
    requestCreateLao(laoName)
      .then((channel: Channel) => subscribeToChannel(channel)
        .then(() => {
          // navigate to the newly created LAO
          navigation.navigate(STRINGS.app_navigation_tab_organizer, {});
        }))
      .catch(
        ((reason) => console.debug(`Failed to establish lao connection: ${reason}`)),
      );
  };

  const selectText = (e: any) => {
    e.target.select();
  };

  return (
    <View style={styleContainer.flex}>
      <View style={styles.viewCenter}>
        <TextBlock text={STRINGS.launch_confirm_description} />
        <TextInput
          style={styles.textInput}
          placeholder={STRINGS.connect_server_uri}
          onChangeText={(input: string) => setServerUrl(input)}
          defaultValue={serverUrl}
          onClick={selectText}
        />
      </View>
      <WideButtonView
        title={STRINGS.general_button_confirm}
        onPress={() => onButtonConfirm()}
      />
      <WideButtonView
        title={STRINGS.general_button_cancel}
        onPress={() => navigation.navigate(STRINGS.launch_navigation_tab_main)}
      />
    </View>
  );
};

const propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

LaunchConfirm.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default LaunchConfirm;
