import React from 'react';
import {
  StyleSheet, View, Text, Button, TextInput,
} from 'react-native';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';
import WebsocketLink from '../websockets/WebsocketLink';
import * as wsApi from '../websockets/WebsocketApi';

/*
* The Launch component
*
* Manage the Launch screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-around',
  },
  text: {
    ...Typography.base,
  },
  textInput: {
    ...Typography.base,
    borderBottomWidth: 2,
  },
  button: {
    paddingHorizontal: Spacing.xl,
    paddingVertical: Spacing.s,
  },
  viewBottom: {
    justifyContent: 'flex-end',
  },
});

let onClickButtonLaunch = () => {

  wsApi.requestCreateLao("Ma petite LAO")
  /*
  const promise = new Promise((resolve, reject) => {
    //wsUtils.requestCreateLao("nom de la LAO", resolve, reject);
    //wsUtils.requestJoinLao("hash de la LAO", resolve, reject);
    //wsUtils.requestCreateEvent("nom de la LAO", "location de la LAO", resolve, reject);
    //wsUtils.requestCastVote("Vote: option number 3", resolve, reject);

    //wsUtils.requestCreateChannel("name of the channel", "contract of the channel", resolve, reject);
    //wsUtils.requestPublishChannel("name of the channel", "content of the event", resolve, reject);
    //wsUtils.requestSubscribeChannel("name of the channel", resolve, reject);
    //wsUtils.requestFetchChannel("name of the channel", "id de l'event", resolve, reject)
  });

  promise.then(
    () => console.log("(TODO) request accepted (launch.js)"),
    (error) => console.error("(TODO) request rejected. Reason :", error)
  );*/
};

const Connect = () => (
  <View style={styles.container}>
    <View style={styles.viewTop}>
      <Text style={styles.text}>{STRINGS.launch_description}</Text>
      <View style={styles.button}>
        <TextInput style={styles.textInput} placeholder={STRINGS.launch_organization_name} />
      </View>
    </View>
    <View style={styles.viewBottom}>
      <View style={styles.button}>
        <Button onPress={() => onClickButtonLaunch()} title={STRINGS.launch_button_launch} />
      </View>
      <View style={styles.button}>
        <Button onPress={() => WebsocketLink.printServerAnswer()} title={STRINGS.general_button_cancel} />
      </View>
    </View>
  </View>
);

export default Connect;
