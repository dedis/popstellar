import React from 'react';
import { StyleSheet, View } from 'react-native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import { Typography } from '../Styles';
import OrganizerEventsCollapsableList from './OrganizerCollapsableList';
import PROPS_TYPE from '../res/Props';

/**
* The Organizer component
*
* Manage the Organizer screen
*/
const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
  },
  text: {
    ...Typography.base,
  },
});

const Organizer = ({ events }) => (
  <View style={styles.container}>
    <OrganizerEventsCollapsableList data={events} closedList={['Future', '']} />
  </View>
);

Organizer.propTypes = {
  events: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(PROPS_TYPE.event).isRequired,
  })).isRequired,
};

const mapStateToProps = (state) => ({
  events: state.currentEventsReducer.events,
});

export default connect(mapStateToProps)(Organizer);
