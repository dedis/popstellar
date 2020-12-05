import React, { useState } from 'react';
import {
  View, Button, Platform, TextInput, StyleSheet, ScrollView, Text,
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { useNavigation } from '@react-navigation/native';

import { Buttons, Typography, Spacing } from '../Styles';
import STRINGS from '../res/strings';

/**
 * Screen to create a roll-call event: a text to explain the meanig of the date field,
 * a deadline date and its button, a description text input, a confirm button, a open button
 * and a cancel buton
 *
 * The deadline time can be choose when the user press on the button on the same row
 *  and it is compulsory
 * The confirm button does noting
 * The open button does noting
 * The cancel button redirect to the organizer component
 *
 * TODO give appropriate name for the button that manage the dates
 * TODO Send the Roll-call event in a future state to the organization server
 *  when the confirm button is press
 * TODO Send the Roll-call event in an open state to the organization server
 *  when the confirm button is press
 */

const styles = StyleSheet.create({
  text: {
    ...Typography.base,
    marginBottom: Spacing.xs,
  },
  button: {
    ...Buttons.base,
  },
  buttonTime: {
    marginHorizontal: Spacing.m,
  },
});

const CreateRollCall = () => {
  const navigation = useNavigation();

  // all the function to manage the date object
  const [startDate, setStartDate] = useState(new Date());
  const [mode, setMode] = useState('date');
  const [show, setShow] = useState(false);

  const showMode = (currentMode) => {
    setMode(currentMode);
    setShow(true);
  };

  const showDatepicker = () => {
    showMode('date');
  };

  const showTimepicker = () => {
    showMode('time');
  };

  const onChange = (event, selectedDate) => {
    const currentDate = selectedDate || startDate;
    setShow(Platform.OS === 'ios');
    setStartDate(currentDate);
    if (event.type === 'set' && mode === 'date') {
      showTimepicker();
    }
  };

  const validate = () => {
    if (mode === 'date') {
      showTimepicker();
    } else {
      setShow(false);
    }
  };

  const dateToStrign = (d) => `${d.getDate()}-${d.getMonth() + 1}-${d.getFullYear()} `
    + `${d.getHours() < 10 ? 0 : ''}${d.getHours()}:`
    + `${d.getMinutes() < 10 ? 0 : ''}${d.getMinutes()}`;

  return (
    <ScrollView>
      <Text style={[styles.text, { textAlign: 'left' }]}>{STRINGS.roll_call_create_deadline}</Text>
      <View style={{ flexDirection: 'row' }}>
        <Text
          style={[styles.text, { flex: 10 }]}
        >
          {dateToStrign(startDate)}
        </Text>
        <View style={[styles.buttonTime, { flex: 1 }]}>
          <Button onPress={() => { showDatepicker(); }} title="S" />
        </View>
      </View>
      {show && (
        <DateTimePicker
          value={startDate}
          mode={mode}
          is24Hour
          display="default"
          onChange={onChange}
        />
      )}
      {show && Platform.OS === 'ios' && (
      <View style={styles.button}>
        <Button title={STRINGS.general_button_confirm} onPress={validate} />
      </View>
      )}
      <TextInput
        style={styles.text}
        placeholder={STRINGS.roll_call_create_description}
      />
      <View style={styles.button}>
        <Button title={STRINGS.general_button_confirm} />
      </View>
      <View style={styles.button}>
        <Button title={STRINGS.general_button_open} />
      </View>
      <View style={styles.button}>
        <Button title={STRINGS.general_button_cancel} onPress={() => { navigation.goBack(); }} />
      </View>
    </ScrollView>
  );
};

export default CreateRollCall;
