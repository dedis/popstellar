import React from 'react';
import {
  StyleSheet, View, Text, Button, TextInput,
} from 'react-native';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';
import PROPS_TYPE from '../res/Props';
import { requestCreateLao } from '../websockets/WebsocketApi'
import { getStore } from '../Store/configureStore';

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

const onButtonLaunchPress = (inputLaoName) => {
  if (inputLaoName.current.value) requestCreateLao(inputLaoName.current.value);
  else requestCreateLao("Ma petite LAO :)"); // TODO temp for testing purposes
  //requestUpdateLao("Nouveau nom");
  //requestStateLao();
  //requestWitnessMessage();
  //requestCreateMeeting("Nouveau meeting", 123, "Lausanne");
  //requestStateMeeting();
};


const Launch = ({ navigation }) => {
  const inputLaoName = React.useRef();

  const cancelAction = () => {
    inputLaoName.current.clear();
    navigation.navigate('Home');
  };

  return (
    <View style={styles.container}>
      <View style={styles.viewTop}>
        <Text style={styles.text}>{STRINGS.launch_description}</Text>
        <View style={styles.button}>
          <TextInput
            ref={inputLaoName}
            style={styles.textInput}
            placeholder={STRINGS.launch_organization_name}
          />
        </View>
      </View>
      <View style={styles.viewBottom}>
        <View style={styles.button}>
          <Button
            title={STRINGS.launch_button_launch}
            onPress={() => onButtonLaunchPress(inputLaoName)}
          />
        </View>
        <View style={styles.button}>
          <Button
            title={"TEST print store"}
            onPress={() => console.log("printing store ", getStore().getState())}
          />
        </View>
        <View style={styles.button}>
          <Button
            title={"CLEAR print store"}
            onPress={() => {
              const a = { type: 'CLEAR_STORAGE' };
              getStore().dispatch(a);
            }}
          />
        </View>
        <View style={styles.button}>
          <Button
            title={STRINGS.general_button_cancel}
            onPress={() => cancelAction()}
          />
        </View>
      </View>
    </View>
  );
};

Launch.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default Launch;
