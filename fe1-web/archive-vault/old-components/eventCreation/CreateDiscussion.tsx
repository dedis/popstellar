import React, { useState } from 'react';
import {
  View, Button, TextInput, StyleSheet, ScrollView,
} from 'react-native';
import { CheckBox } from 'react-native-elements';
import { useNavigation } from '@react-navigation/native';

import { Buttons, Typography, Spacing } from '../../../styles/index';
import STRINGS from '../../../res/strings';

/**
 * Screen to create a discussion event: a name text input, a discussion open checkbox,
 * a confirm button and a cancel button.
 *
 * The name text input is a compulsory field and it is the name of the discussion
 * The discussion open checkbox says if the participant can participate
 *  or if the discussion is closed
 * The confirm button does nothing, enable only when the discussion has a name
 * The cancel button go back to the Organizer component
 *
 * TODO Send the disscusion event to the organization server when the confirm button is press
 * TODO must be modify when the discussion event will be describe in UI Specifications
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
