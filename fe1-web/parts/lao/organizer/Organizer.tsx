import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import EventListCollapsible from 'components/eventList/EventListCollapsible';
import LaoProperties from 'components/eventList/LaoProperties';
import PROPS_TYPE from 'res/Props';
import { ScrollView } from 'react-native';

const laoToProperties = (events: any) => [[], ...events];

/**
 * Organizer screen: lists editable LAO properties and past/ongoing/future events
 *
 * TODO By default only the past and present section are open.
 * TODO use the data received by the organization server
*/
const Organizer = (props: IPropTypes) => {
  const { events } = props;
  const data = laoToProperties(events);

  const DATA_EXAMPLE = [ // TODO refactor when Event storage available
    {
      title: 'Past',
      data: [(data[1].data)[0], (data[1].data)[1], (data[1].data)[2]],
    },
    {
      title: 'Present',
      data: [(data[2].data)[0], (data[2].data)[1], (data[2].data)[2]],
    },
    {
      title: 'Future',
      data: [(data[3].data)[0]],
    },
  ];

  return (
    <ScrollView>
      { /* Add edit button for the organizer in the Lao properties panel */ }
      <LaoProperties />
      <EventListCollapsible isOrganizer data={DATA_EXAMPLE} />
    </ScrollView>
  );
};

const propTypes = {
  events: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(
      PropTypes.oneOfType([PROPS_TYPE.event, PROPS_TYPE.property]),
    ).isRequired,
  })).isRequired,
};
Organizer.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const mapStateToProps = (state: any) => ({
  events: state.currentEvents.events,
});

export default connect(mapStateToProps)(Organizer);
