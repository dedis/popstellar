import React, { useState } from 'react';
import {
  View, Button, TextInput, StyleSheet, FlatList, Text, Platform,
} from 'react-native';
import DateTimePicker from '@react-native-community/datetimepicker';
import RadioForm from 'react-native-simple-radio-button';
import { useNavigation } from '@react-navigation/native';

import { Buttons, Typography, Spacing } from '../Styles';
import STRINGS from '../res/strings';

/**
 * Screen to create a poll event
 *
 * It is possible to put a finish time before the start time => to fix
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

const radioProps = [
  { label: 'Choose one of n', value: 0 },
  { label: 'Approve any of n', value: 1 },
];

const CreatePoll = () => {
  const navigation = useNavigation();
  const [name, setName] = useState('');
  const [answers, setAnswers] = useState(['']);
  const [radioOrCheckbox, setRadioOrCheckbox] = useState(0);

  const [startDate, setStartDate] = useState(new Date());
  const [finishDate, setFinishDate] = useState();
  const [settingStart, setSettingStart] = useState(true);
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
      setFinishDate(new Date());
    }
    setSettingStart(false);
    showDatepicker();
  };

  const removeElement = (index) => {
    if (answers.length > 1) {
      answers.splice(index, 1);
      setAnswers([...answers]);
    }
  };

  const updateText = (k, t) => {
    answers[k] = t;
    if (k === answers.length - 1 && t !== '') {
      answers.push('');
    }
    setAnswers([...answers]);
  };

  const dateToStrign = (d) => `${d.getDate()}-${d.getMonth() + 1}-${d.getFullYear()} `
    + `${d.getHours() < 10 ? 0 : ''}${d.getHours()}:`
    + `${d.getMinutes() < 10 ? 0 : ''}${d.getMinutes()}`;

  return (
    <FlatList
      data={answers}
      keyExtractor={(item, index) => index.toString()}
      renderItem={({ index }) => (
        <View style={{ flexDirection: 'row' }}>
          <TextInput
            style={[styles.text, { flex: 10 }]}
            placeholder={`Answer ${index + 1}`}
            onChangeText={(text) => updateText(index, text)}
            value={answers[index]}
          />
          <View style={[styles.button, { flex: 1 }]}>
            {!(index === answers.length - 1 && answers[index] === '') && (
              <Button onPress={() => removeElement(index)} title="-" />
            )}
          </View>
        </View>
      )}
      ListHeaderComponent={(
        <View>
          <TextInput
            style={styles.text}
            placeholder={STRINGS.poll_create_question}
            onChangeText={(text) => { setName(text); }}
          />
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
          <View style={{ flexDirection: 'row' }}>
            <Text
              style={[styles.text, { flex: 10 }]}
            >
              {finishDate !== undefined ? dateToStrign(finishDate)
                : STRINGS.poll_create_finish_time}
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
            <Button title={STRINGS.connect_confirm_description} onPress={validate} />
          </View>
          )}
          <RadioForm
            radio_props={radioProps}
            initial={0}
            onPress={(val) => setRadioOrCheckbox(val)}
            style={{ marginLeft: Spacing.s }}
          />
        </View>
      )}
      ListFooterComponent={(
        <View style={{ marginVertical: Spacing.xl }}>
          <View style={styles.button}>
            <Button
              title={STRINGS.connect_confirm_description}
              isabled={name === '' || answers.length < 3}
            />
          </View>
          <View style={styles.button}>
            <Button
              title={STRINGS.general_button_open}
              disabled={name === '' || new Date() < startDate || answers.length < 3}
            />
          </View>
          <View style={styles.button}>
            <Button
              title={STRINGS.general_button_cancel}
              onPress={() => { navigation.goBack(); }}
            />
          </View>
        </View>
      )}
      listKey="pollList"
    />
  );
};

export default CreatePoll;
