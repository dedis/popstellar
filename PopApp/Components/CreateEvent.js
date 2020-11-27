import { useNavigation } from '@react-navigation/native';
import React, { useState } from 'react';
import {
  StyleSheet, View, Text, Button,
} from 'react-native';

import CreateMeeting from './CreateMeeting';
import CreateDiscussion from './CreateDiscussion';
import CreatePoll from './CreatePoll';
import CreateRollCall from './CreateRollCall';
import { Buttons, Typography } from '../Styles';
import STRINGS from '../res/strings';

/**
* The Create event component
*/
const styles = StyleSheet.create({
  view: {
    flex: 1,
    justifyContent: 'space-evenly',
  },
  text: {
    ...Typography.important,
  },
  button: {
    ...Buttons.base,
  },
});

const CreateEvent = () => {
  const eventType = ['Meeting', 'Roll-Call', 'Discussion', 'Poll'];
  const [type, setType] = useState(undefined);
  const navigation = useNavigation();

  switch (type) {
    case 'Meeting':
      return (
        <CreateMeeting />
      );
    case 'Roll-Call':
      return (
        <CreateRollCall />
      );
    case 'Discussion':
      return (
        <CreateDiscussion />
      );
    case 'Poll':
      return (
        <CreatePoll />
      );
    default:
      return (
        <View style={styles.view}>
          <Text style={styles.text}>{STRINGS.create_description}</Text>
          {eventType.map((element) => (
            <View style={styles.button} key={element}>
              <Button title={element} onPress={() => { setType(element); }} />
            </View>
          ))}
          <View style={styles.button}>
            <Button
              title={STRINGS.general_button_cancel}
              onPress={() => { setType(undefined); navigation.goBack(); }}
            />
          </View>
        </View>
      );
  }
};

export default CreateEvent;
