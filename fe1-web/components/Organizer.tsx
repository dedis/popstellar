import React from 'react';
import { StyleSheet, View } from 'react-native';
import PropTypes from 'prop-types';
import { Dispatch } from 'redux';
import { connect, useSelector } from 'react-redux';
import { useNavigation } from '@react-navigation/native';

import { disconnectFromLao as disconnectFromLaoAction, makeCurrentLao } from 'store';
import { Lao } from 'model/objects';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import { Typography } from 'styles';

import OrganizerEventsCollapsableList from './OrganizerCollapsableList';

/**
 * Manage the Organizer screen: A section list of propreties and events
 *
 * The section list show the events and propreties of the LAO open in
 * the organitation UI.
 *
 * By default only the past and present section are open.
 *
 * TODO use the data receive by the organization server
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

const laoToProperties = (events, lao: Lao) => {
  const name = { id: 'organization_name', object: 'organization_name', name: lao.name };
  const witness = { id: 'witness', object: 'witness', witnesses: lao.witnesses };
  const properties = { title: '', data: [name, witness] };
  return [properties, ...events];
};

const Organizer = ({ events, dispatch }) => {
  const currentLao = makeCurrentLao();
  const lao = useSelector(currentLao);
  if (!lao) {
    const action = { type: 'APP_NAVIGATION_OFF' };
    dispatch(action);
    useNavigation().navigate(STRINGS.app_navigation_tab_home);
  }
  return (
    <View style={styles.container}>
      <OrganizerEventsCollapsableList data={laoToProperties(events, lao)} closedList={['Future', '']} />
    </View>
  );
};

Organizer.propTypes = {
  events: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(
      PropTypes.oneOfType([PROPS_TYPE.event, PROPS_TYPE.property]),
    ).isRequired,
  })).isRequired,
  dispatch: PropTypes.func.isRequired,
};

const mapStateToProps = (state) => ({
  events: state.currentEvents.events,
});

const mapDispatchToProps = (dispatch: Dispatch) => ({
  // dispatching actions returned by action creators
  disconnectFromLao: () => dispatch(disconnectFromLaoAction()),
});

const OrganizerContainer = connect(mapStateToProps, mapDispatchToProps)(Organizer);
OrganizerContainer.propTypes = {
  events: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(
      PropTypes.oneOfType([PROPS_TYPE.event, PROPS_TYPE.property]),
    ).isRequired,
  })).isRequired,
};

export default OrganizerContainer;
