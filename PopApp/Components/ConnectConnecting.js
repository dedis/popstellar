import React from 'react';
import {
  StyleSheet, View, Text, Button, ActivityIndicator,
} from 'react-native';

import STRINGS from '../res/strings';
import { Buttons, Colors, Typography } from '../Styles';
import PROPS_TYPE from '../res/Props';

/**
 *  Connect to a LAO
 *
 *  Currently, just simulate waiting for a response
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
            navigation.navigate('Scanning');
          }}
        />
      </View>
      <View style={styles.button}>
        <Button
          title={STRINGS.connect_connecting_validate}
          onPress={() => {
            navigation.navigate('Confirm');
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
