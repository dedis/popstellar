import React, { useState } from 'react';
import {
  View, Button, Platform, TextInput, StyleSheet, ScrollView, Text,
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { useNavigation } from '@react-navigation/native';

import {
  Buttons, Typography, Spacing, Views,
} from '../Styles';
import STRINGS from '../res/strings';
import { requestCreateRollCall } from '../websockets/WebsocketApi';

/**
 * Screen to create a roll-call event: a text to explain the meaning of the date field,
 * a deadline date and its button, a description text input, a confirm button, a open button
 * and a cancel buton
 *
 * The deadline time can be choose when the user press on the button on the same row
 *  and it is compulsory
 * The confirm button does noting
 * The open button does noting
 * The cancel button redirect to the organizer component
 *
 * FIXME give appropriate name for the button that manage the dates
 * FIXME Send the Roll-call event in a future state to the organization server
 *  when the confirm button is press
 * FIXME Send the Roll-call event in an open state to the organization server
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
  view: {
    ...Views.base,
    flexDirection: 'row',
    zIndex: 3,
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

  const [description, setDescription] = useState('');
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');

  // FIXME: there seems to be a lot of different logic across platforms & we here
  // Can it be more or less unified?
  return (
    <ScrollView>
      {Platform.OS !== 'web'
      && (
      <View>
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
      </View>
      )}
      {Platform.OS === 'web'
      && (
      <View style={[styles.view, styles.zIndexBooster]}>
        <Text style={styles.text}>{STRINGS.roll_call_create_deadline}</Text>
        <DatePicker
          selected={startDate}
          onChange={(date) => setStartDate(date)}
          imeInputLabel="Time:"
          dateFormat="MM/dd/yyyy HH:mm"
          showTimeInput
        />
      </View>
      )}
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
        placeholder={STRINGS.roll_call_create_name}
        onChangeText={(text) => { setName(text); }}
      />
      <TextInput
        style={styles.text}
        placeholder={STRINGS.roll_call_create_location}
        onChangeText={(text) => { setLocation(text); }}
      />
      <TextInput
        style={styles.text}
        placeholder={STRINGS.roll_call_create_description}
        onChangeText={(text) => { setDescription(text); }}
      />
      <View style={styles.button}>
        <Button
          title={STRINGS.general_button_confirm}
          onPress={() => {
            requestCreateRollCall(name, location, -1, startDate, description);
            navigation.goBack();
          }}
          disabled={name.trim() === '' || location.trim() === ''}
        />
      </View>
      <View style={styles.button}>
        <Button
          title={STRINGS.general_button_open}
          onPress={() => {
            requestCreateRollCall(name, location, Math.floor(Date.now() / 1000), -1, description);
            navigation.goBack();
          }}
          disabled={name.trim() === '' || location.trim() === ''}
        />
      </View>
      <View style={styles.button}>
        <Button title={STRINGS.general_button_cancel} onPress={() => { navigation.goBack(); }} />
      </View>
    </ScrollView>
  );
};

export default CreateRollCall;
