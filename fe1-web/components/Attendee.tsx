import React from 'react';
import {
  StyleSheet, View,
} from 'react-native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import { useNavigation } from '@react-navigation/native';

import PROPS_TYPE from 'res/Props';
import STRINGS from 'res/strings';
import { Lao } from 'model/objects';
import EventsCollapsableList from './EventsCollapsableList';

/**
 * Manage the Attendee screen: A section list of propreties and events
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
});

const laoToProperties = (events: any, lao: Lao) => {
  const name = { id: 'organization_name', object: 'organization_name', name: lao.name };
  const witness = { id: 'witness', object: 'witness', witnesses: lao.witnesses };
  const properties = { title: '', data: [name, witness] };
  return [properties, ...events];
};

const propTypes = {
  events: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(
      PropTypes.oneOfType([PROPS_TYPE.event, PROPS_TYPE.property]),
    ).isRequired,
  })).isRequired,
  lao: PROPS_TYPE.LAO.isRequired,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const Attendee = ({ events, lao }: IPropTypes) => {
  if (!lao.name || !lao.witnesses) {
    useNavigation().navigate(STRINGS.app_navigation_tab_home);
  }
  return (
    <View style={styles.container}>
      <EventsCollapsableList
        data={laoToProperties(events, lao)}
        closedList={['Future', '']}
      />
    </View>
  );
};

Attendee.propTypes = propTypes;

const mapStateToProps = (state: any) => ({
  events: state.currentEvents.events,
  lao: state.openedLao,
});

export default connect(mapStateToProps)(Attendee);
