import React from 'react';
import {
  StyleSheet, View,
} from 'react-native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import EventsCollapsableList from './EventsCollapsableList';
import PROPS_TYPE from '../res/Props';

/**
* The Attendee component
*
* Manage the Attendee screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
});

const Attendee = ({ events }) => (
  <View style={styles.container}>
    <EventsCollapsableList
      data={events}
      closedList={['Future', '']}
    />
  </View>
);

Attendee.propTypes = {
  events: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(PROPS_TYPE.event).isRequired,
  })).isRequired,
};

const mapStateToProps = (state) => ({
  events: state.currentEventsReducer.events,
});

export default connect(mapStateToProps)(Attendee);
