import React from 'react';
import {
  StyleSheet, View, Text, FlatList, Button,
} from 'react-native';
import { useNavigation } from '@react-navigation/native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import { Buttons, Spacing } from 'styles';
import PROPS_TYPE from 'res/Props';

import EventItem from './EventItem';

/**
 * Organizer Roll-call component: a status (Future, Open, Closed), the number of participants,
 * an open button and a list of the nested events
 *
 * The open button allow the organizer to scan the attendee
 *
 * TODO use data of the organizer server
 * TODO send open request when click on the open button
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
  flatList: {
    marginTop: Spacing.xs,
  },
  buttons: {
    ...Buttons.base,
  },
});

const RollCallEventOrganizer = ({ event, dispatch }) => {
  const navigation = useNavigation();

  const getState = () => {
    if (event.scheduled) {
      return 'Future';
    }
    if (event.end) {
      return 'Close';
    }
    return 'Open';
  };

  return (
    <View style={styles.view}>
      <Text style={styles.text}>{`Status: ${getState()}`}</Text>
      <Text style={styles.text}>Participants #</Text>
      {!(!event.scheduled && !event.end) && (
        <View style={styles.buttons}>
          <Button
            title="Open"
            onPress={() => {
              const action = { type: 'SET_OPEN_ROLL_CALL_ID', value: event.id };
              dispatch(action);
              navigation.navigate('Roll-Call');
            }}
          />
        </View>
      )}
      <FlatList
        data={event.children}
        keyExtractor={(item) => item.id.toString()}
        renderItem={({ item }) => <EventItem event={item} />}
        listKey={`RollCallEventOrganizer-${event.id.toString()}`}
        style={styles.flatList}
      />
    </View>
  );
};

RollCallEventOrganizer.propTypes = {
  event: PROPS_TYPE.event.isRequired,
  dispatch: PropTypes.func.isRequired,
};

const mapStateToProps = (state) => ({
  roll_call_id: state.getOpenRollCallId.roll_call_id,
});

export default connect(mapStateToProps)(RollCallEventOrganizer);
