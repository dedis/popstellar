import React, { useState } from 'react';
import {
  StyleSheet, ScrollView, Text, TextInput,
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
  text: {
    ...Typography.base,
    marginBottom: Spacing.xs,
  },
});

function QRCode({ visible, name }) {
  if (visible && name && name.trim().length) {
    return <Text style={styles.text} d>QR code</Text>;
  }
  return null;
}

QRCode.propTypes = {
  visible: PropTypes.bool.isRequired,
  name: PropTypes.string.isRequired,
};

const Identity = () => {
  const [toggleCheckBox, setToggleCheckBox] = useState(true);
  const textColor = toggleCheckBox ? Colors.gray : Colors.black;
  const [name, setName] = useState('');

  const anonymousPress = () => {
    setToggleCheckBox(!toggleCheckBox);
  };

  return (
    <ScrollView>
      <CheckBox
        checked={toggleCheckBox}
        onPress={() => anonymousPress()}
        title={STRINGS.identity_check_box_anonymous}
      />
      <Text style={styles.text}>{STRINGS.identity_check_box_anonymous_description}</Text>
      <TextInput
        style={[styles.text, { color: textColor }]}
        placeholder={STRINGS.identity_name_placeholder}
        editable={!toggleCheckBox}
        onChangeText={(text) => { setName(text); }}
      />
      <TextInput
        style={[styles.text, { color: textColor }]}
        placeholder={STRINGS.identity_title_placeholder}
        editable={!toggleCheckBox}
      />
      <TextInput
        style={[styles.text, { color: textColor }]}
        placeholder={STRINGS.identity_organization_placeholder}
        editable={!toggleCheckBox}
      />
      <TextInput
        style={[styles.text, { color: textColor }]}
        autoCompleteType="email"
        placeholder={STRINGS.identity_email_placeholder}
        keyboardType="email-address"
        editable={!toggleCheckBox}
      />
      <TextInput
        style={[styles.text, { color: textColor }]}
        autoCompleteType="tel"
        dataDetectorTypes="phoneNumber"
        placeholder={STRINGS.identity_phone_placeholder}
        keyboardType="phone-pad"
        editable={!toggleCheckBox}
      />
      <QRCode visible={!toggleCheckBox} name={name} />
    </ScrollView>
  );
};

export default Identity;
