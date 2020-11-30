import React from 'react';
import {
  StyleSheet, View, Text, Button, TextInput,
} from 'react-native';

import STRINGS from '../res/strings';
import { Spacing, Typography } from '../Styles';
import PROPS_TYPE from '../res/Props';
import { requestCreateLao } from '../websockets/WebsocketApi'

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

const Launch = ({ navigation }) => {
  const LAOName = React.useRef();

  const cancelAction = () => {
    LAOName.current.clear();
    navigation.navigate('Home');
  };

  return (
    <View style={styles.container}>
      <View style={styles.viewTop}>
        <Text style={styles.text}>{STRINGS.launch_description}</Text>
        <View style={styles.button}>
          <TextInput
            ref={LAOName}
            style={styles.textInput}
            placeholder={STRINGS.launch_organization_name}
          />
        </View>
      </View>
      <View style={styles.viewBottom}>
        <View style={styles.button}>
          <Button
            title={STRINGS.launch_button_launch}
            onPress={() => requestCreateLao("Ma petite LAO :)")}
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
