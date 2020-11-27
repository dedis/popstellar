import React, { useState } from 'react';
import {
  StyleSheet, View, Text, FlatList,
} from 'react-native';
import RadioForm from 'react-native-simple-radio-button';
import { CheckBox } from 'react-native-elements';

import EventItem from './EventItem';
import ProgressBar from './ProgressBar';
import { Spacing } from '../Styles';
import PROPS_TYPE from '../res/Props';

/**
 * Poll component
 */
const styles = StyleSheet.create({
  view: {
    marginHorizontal: Spacing.s,
    borderWidth: 1,
    borderRadius: 5,
    paddingHorizontal: Spacing.xs,
    marginBottom: Spacing.xs,
  },
  text: {
  },
  progress: {
    flexDirection: 'row',
  },
  flatList: {
    marginTop: Spacing.xs,
  },
});

const radioProps = [
  { label: 'param1', value: 0 },
  { label: 'param2', value: 1 },
  { label: 'param3', value: 2 },
  { label: 'param4', value: 3 },
];

const radioPropsPercent = [
  { label: 'param1', value: 0.5 },
  { label: 'param2', value: 0.3 },
  { label: 'param3', value: 0.0 },
  { label: 'param4', value: 0.2 },
];

const PollEvent = ({ event }) => {
  const [radioValue, setRadioValue] = useState(0);
  const map = new Map();
  radioProps.forEach((e) => map.set(e.label, false));
  const [buttonValues, setButtonValues] = useState(map);

  return (
    <View style={styles.view}>
      <Text style={styles.text}>{event.name}</Text>
      <Text style={styles.text}>Status (Future, Open or Closed)</Text>
      <Text style={styles.text}>Participants: N of M</Text>
      <RadioForm
        radio_props={radioProps}
        initial={0}
        onPress={(val) => { setRadioValue(val); }}
      />
      {radioProps.map((e) => (
        <CheckBox
          key={e.label}
          checked={buttonValues.get(e.label)}
          onPress={() => {
            const newVal = !buttonValues.get(e.label);
            setButtonValues(new Map(buttonValues.set(e.label, newVal)));
          }}
          title={e.label}
        />
      ))}
      {radioPropsPercent.map((e) => (
        <View style={styles.progress} key={e.label}>
          <Text>{e.label}</Text>
          <ProgressBar
            progress={e.value}
            style={{ flex: 1 }}
          />
        </View>
      ))}
      <FlatList
        data={event.childrens}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <EventItem event={item} />}
        listKey={event.id.toString()}
        style={styles.flatList}
      />
    </View>
  );
};

PollEvent.propTypes = {
  event: PROPS_TYPE.event.isRequired,
};

export default PollEvent;
