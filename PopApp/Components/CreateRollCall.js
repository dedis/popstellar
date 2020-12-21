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

/**
 * Screen to create a roll-call event
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
