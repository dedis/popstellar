import React, { useState } from 'react';
import {
  StyleSheet, View, Text, TextInput,
} from 'react-native';
import { CheckBox } from 'react-native-elements';
import PropTypes from 'prop-types';

import STRINGS from '../res/strings';
import { Colors, Spacing, Typography } from '../Styles';

/**
* The Identity component
*
* Manage the Identity screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  text: {
    ...Typography.base,
    marginBottom: Spacing.xs,
  },
});

function QRCode({ visible }) {
  if (visible) {
    return <Text style={styles.text} d>QR code</Text>;
  }
  return null;
}

QRCode.propTypes = {
  visible: PropTypes.bool.isRequired,
};

const Identity = () => {
  const [toggleCheckBox, setToggleCheckBox] = useState(true);

  return (
    <View style={styles.container}>
      <CheckBox
        checked={toggleCheckBox}
        onPress={() => setToggleCheckBox(!toggleCheckBox)}
        title={STRINGS.identity_check_box_anonymous}
      />
      <Text style={styles.text}>{STRINGS.identity_check_box_anonymous_description}</Text>
      <TextInput style={[styles.text, { color: toggleCheckBox ? Colors.gray : Colors.black }]} autoCompleteType="name" placeholder="Name" editable={!toggleCheckBox} />
      <TextInput style={[styles.text, { color: toggleCheckBox ? Colors.gray : Colors.black }]} autoCompleteType="off" placeholder="Title" editable={!toggleCheckBox} />
      <TextInput style={[styles.text, { color: toggleCheckBox ? Colors.gray : Colors.black }]} autoCompleteType="off" placeholder="Organization" editable={!toggleCheckBox} />
      <TextInput style={[styles.text, { color: toggleCheckBox ? Colors.gray : Colors.black }]} autoCompleteType="email" placeholder="Email" keyboardType="email-address" editable={!toggleCheckBox} />
      <TextInput style={[styles.text, { color: toggleCheckBox ? Colors.gray : Colors.black }]} autoCompleteType="tel" dataDetectorTypes="phoneNumber" placeholder="Phone number" keyboardType="phone-pad" editable={!toggleCheckBox} />
      <QRCode visible={!toggleCheckBox} />
    </View>
  );
};

export default Identity;
