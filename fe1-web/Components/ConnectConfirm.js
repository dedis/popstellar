import React from 'react';
import {
  StyleSheet, View, ScrollView, Text, Button,
} from 'react-native';

import { Spacing, Typography } from '../styles';
import STRINGS from '../res/strings';
import PROPS_TYPE from '../res/Props';

/**
 * Ask for confirmation to connect to LAO: An info ScrollView,
 * a confirm button and a cancel button
 *
 * The ScrollView show information for the user can verify it
 * connect to the good organization server
 *
 * The confirm button navigates to the Lauch tab.
 * The cancel button navigates back to the ConnectScanning component
 *
 * TODO Make the confirm button make the action require in the UI specification
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  text: {
    ...Typography.base,
  },
  buttonView: {
    flex: 2,
    flexDirection: 'column',
    marginBottom: Spacing.xl,
  },
  button: {
    padding: Spacing.xs,
  },
  viewCenter: {
    flex: 8,
    justifyContent: 'center',
    borderWidth: 1,
    margin: Spacing.xs,
  },
  view: {
    flex: 1,
    justifyContent: 'center',
  },
});

const ConnectConfirm = ({ navigation }) => (
  <View style={styles.container}>
    <View style={styles.view}>
      <Text style={styles.text}>{STRINGS.connect_confirm_description}</Text>
    </View>
    <View style={styles.viewCenter}>
      <ScrollView>
        <Text style={styles.text}>{STRINGS.lorem_ipsum}</Text>
      </ScrollView>
    </View>
    <View style={styles.buttonView}>
      <View style={styles.button}>
        <Button
          title={STRINGS.general_button_confirm}
          style={styles.button}
          onPress={() => {
            const parentNavigation = navigation.dangerouslyGetParent();
            if (parentNavigation !== undefined) {
              parentNavigation.navigate(STRINGS.navigation_tab_launch);
            }
          }}
        />
      </View>
      <View style={styles.button}>
        <Button
          title={STRINGS.general_button_cancel}
          style={styles.button}
          onPress={() => { navigation.navigate(STRINGS.connect_scanning_title); }}
        />
      </View>
    </View>
  </View>
);

ConnectConfirm.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default ConnectConfirm;
