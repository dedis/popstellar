import React, { useState } from 'react';
import {
  View, Button, Platform, TextInput, StyleSheet, ScrollView, Text,
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import { useNavigation } from '@react-navigation/native';
import DatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';

import {
  Buttons, Typography, Spacing, Views,
} from '../Styles';
import STRINGS from '../res/strings';

/**
 * Screen to create a meeting event
 *
 * It is possible to put a finish time before the start time => to fix
 * Design not finish
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
  zIndexBooster: {
    zIndex: 100,
  },
});

const CreateMeeting = () => {
  const navigation = useNavigation();

  const [startDate, setStartDate] = useState(new Date());
  const [finishDate, setFinishDate] = useState();
  const [settingStart, setSettingStart] = useState(true);
  const [mode, setMode] = useState('date');
  const [show, setShow] = useState(false);

  const setEndDate = (date) => {
    if (date < startDate) {
      setFinishDate(startDate);
    } else {
      setFinishDate(date);
    }
  };

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
    const currentDate = selectedDate || (settingStart ? startDate : finishDate);
    setShow(Platform.OS === 'ios');
    if (settingStart) {
      setStartDate(currentDate);
    } else {
      setFinishDate(currentDate);
    }
    if (event.type === 'set' && mode === 'date') {
      showTimepicker();
    }
    if (event.type === 'set' && mode === 'time' && finishDate && finishDate <= startDate) {
      setFinishDate(startDate);
    }
  };

  const validate = () => {
    if (mode === 'date') {
      showTimepicker();
    } else {
      setShow(false);
    }
  };

  const selectFinish = () => {
    if (finishDate === undefined) {
      setFinishDate(startDate);
    }
    setSettingStart(false);
    showDatepicker();
  };

  const dateToStrign = (d) => `${d.getDate()}-${d.getMonth() + 1}-${d.getFullYear()} `
    + `${d.getHours() < 10 ? 0 : ''}${d.getHours()}:`
    + `${d.getMinutes() < 10 ? 0 : ''}${d.getMinutes()}`;

  const [name, setName] = useState('');

  return (
    <ScrollView>
      <TextInput
        style={styles.text}
        placeholder={STRINGS.meeting_create_name}
        onChangeText={(text) => { setName(text); }}
      />
      {Platform.OS !== 'web'
      && (
      <View style={{ flexDirection: 'row' }}>
        <Text
          style={[styles.text, { flex: 10 }]}
        >
          {dateToStrign(startDate)}
        </Text>
        <View style={[styles.buttonTime, { flex: 1 }]}>
          <Button onPress={() => { setSettingStart(true); showDatepicker(); }} title="S" />
        </View>
      </View>
      )}
      {Platform.OS === 'web'
      && (
      <View style={[styles.view, styles.zIndexBooster]}>
        <Text style={styles.text}>{STRINGS.meeting_create_start_time}</Text>
        <DatePicker
          selected={startDate}
          onChange={(date) => {
            setStartDate(date);
            if (finishDate < date) {
              setFinishDate(date);
            }
          }}
          imeInputLabel="Time:"
          dateFormat="MM/dd/yyyy HH:mm"
          showTimeInput
        />
      </View>
      )}
      {Platform.OS !== 'web'
      && (
      <View style={{ flexDirection: 'row' }}>
        <Text
          style={[styles.text, { flex: 10 }]}
        >
          {finishDate !== undefined ? dateToStrign(finishDate) : STRINGS.meeting_create_finish_time}
        </Text>
        <View style={[{ flexDirection: 'row', flex: 2 }, styles.buttonTime]}>
          <View style={{ marginRight: Spacing.xs }}>
            <Button onPress={selectFinish} title="S" />
          </View>
          <View>
            <Button onPress={() => setFinishDate(undefined)} title="C" />
          </View>
        </View>
      </View>
      )}
      {Platform.OS === 'web'
      && (
      <View style={styles.view}>
        <Text style={styles.text}>{STRINGS.meeting_create_finish_time}</Text>
        <DatePicker
          selected={finishDate}
          onChange={(date) => setEndDate(date)}
          imeInputLabel="Time:"
          dateFormat="MM/dd/yyyy HH:mm"
          showTimeInput
        />
        <View style={{ marginLeft: Spacing.m }}>
          <Button onPress={() => setFinishDate(undefined)} title="Clear" />
        </View>
      </View>
      )}
      {show && (
        <DateTimePicker
          value={settingStart ? startDate : finishDate}
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
        placeholder={STRINGS.meeting_create_location}
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

export default CreateMeeting;
