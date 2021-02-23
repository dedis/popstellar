import React from 'react';
import {
  StyleSheet, View, Text, Button, ViewStyle, TextStyle,
} from 'react-native';

import { Spacing, Typography } from 'styles';
import STRINGS from 'res/strings';
import PROPS_TYPE, { INavigation } from 'res/Props';

/**
 * The unapproved component: a explain string and a persmission button
 *
 * The explain string say the app need to use the camera
 * The permission button naviagtes to the ConnectScanning component
 *
 * TODO ask the user for the camera permission when click on the permission button
 * if he accept navigates to the ConnectScanning component
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'space-evenly',
  },
  text: {
    ...Typography.base,
  } as TextStyle,
  button: {
    marginHorizontal: Spacing.xl,
  } as ViewStyle,
});

interface IPropTypes {
  navigation: INavigation;
}

const ConnectUnapprove = ({ navigation }: IPropTypes) => (
  <View style={styles.container}>
    <Text style={styles.text}>{STRINGS.connect_description}</Text>
    <View style={styles.button}>
      <Button
        title={STRINGS.connect_button_camera}
        onPress={() => { navigation.navigate(STRINGS.connect_scanning_title); }}
      />
    </View>
  </View>
);

ConnectUnapprove.propTypes = {
  navigation: PROPS_TYPE.navigation.isRequired,
};

export default ConnectUnapprove;
