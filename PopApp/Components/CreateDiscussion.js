import React, { useState } from 'react';
import {
  View, Button, TextInput, StyleSheet, ScrollView,
} from 'react-native';
import { CheckBox } from 'react-native-elements';
import { useNavigation } from '@react-navigation/native';

import { Buttons, Typography, Spacing } from '../Styles';
import STRINGS from '../res/strings';

/**
 * Screen to create a discussion event
 */

const styles = StyleSheet.create({
  text: {
    ...Typography.base,
    marginBottom: Spacing.xs,
  },
  button: {
    ...Buttons.base,
  },
});

const CreateDiscussion = () => {
  const navigation = useNavigation();
  const [isOpen, setOpen] = useState(true);
  const [name, setName] = useState('');

  return (
    <ScrollView>
      <TextInput
        style={styles.text}
        placeholder={STRINGS.discussion_create_name}
        onChangeText={(text) => { setName(text); }}
      />
      <CheckBox
        checked={isOpen}
        onPress={() => setOpen(!isOpen)}
        title={STRINGS.discussion_create_open}
      />
      <View style={styles.button}>
        <Button title={STRINGS.general_button_confirm} disabled={name === ''} />
      </View>
      <View style={styles.button}>
        <Button title={STRINGS.general_button_cancel} onPress={() => { navigation.goBack(); }} />
      </View>
    </ScrollView>
  );
};

export default CreateDiscussion;
