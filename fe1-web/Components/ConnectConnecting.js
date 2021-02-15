import React from 'react';
import {
  StyleSheet, View, Text, Button, ActivityIndicator,
} from 'react-native';

import STRINGS from '../res/strings';
import { Buttons, Colors, Typography } from '../styles';
import PROPS_TYPE from '../res/Props';

/**
 * Connect to a LAO: An activity indicator, a cancel button and a simulate validation button
 *
 * Currently, just simulate waiting for a response
 *
 * The cancel button go back to the ConnectScanning component
 * The simulate connection button go to the ConnectConfirm component
 *
 * TODO make the screen to perform a request to the organizer
 * server to verify if the user can connect and go to the ConnectConfirm with the information
 * receive by the server
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-evenly',
  },
  text: {
    ...Typography.base,
  },
  button: {
    ...Buttons.base,
  },
});

const ConnectConnecting = ({ navigation }) => (
  <View style={styles.container}>
    <View>
      <Text style={styles.text}>{STRINGS.connect_connecting_uri}</Text>
    </View>
    <View>
      <View>
        <ActivityIndicator size="large" color={Colors.blue} />
      </View>
    </View>
    <View>
      <View style={styles.button}>
        <Button
          title={STRINGS.general_button_cancel}
          onPress={() => {
            navigation.navigate(STRINGS.connect_scanning_title);
          }}
        />
      </View>
      <View style={styles.button}>
        <Button
          title={STRINGS.connect_connecting_validate}
          onPress={() => {
            navigation.navigate(STRINGS.connect_confirm_title);
          }}
        />
      </View>
    </View>
  </View>
);

ConnectConnecting.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default ConnectConnecting;
