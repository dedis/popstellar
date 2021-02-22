import React from 'react';
import { StyleSheet, View } from 'react-native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { useNavigation } from '@react-navigation/native';

import STRINGS from 'res/strings';
import { Typography } from '../styles';
import OrganizerEventsCollapsableList from './OrganizerCollapsableList';
import PROPS_TYPE from '../res/Props';

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

const laoToProperties = (events, lao) => {
  const name = { id: 'organization_name', object: 'organization_name', name: lao.name };
  const witness = { id: 'witness', object: 'witness', witnesses: lao.witnesses };
  const properties = { title: '', data: [name, witness] };
  return [properties, ...events];
};

const Organizer = ({ events, lao, dispatch }) => {
  if (!lao.name || !lao.witnesses) {
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
  lao: PROPS_TYPE.LAO.isRequired,
  dispatch: PropTypes.func.isRequired,
};

const mapStateToProps = (state) => ({
  events: state.currentEvents.events,
  lao: state.openedLao.lao,
});

export default connect(mapStateToProps)(Organizer);
