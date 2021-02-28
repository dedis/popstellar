import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import PROPS_TYPE from 'res/Props';
import EventListCollapsible from 'components/eventList/EventListCollapsible';
import LaoProperties from 'components/eventList/LaoProperties';

const laoToProperties = (events: any) => [[], ...events];

/**
 * Attendee screen: lists LAO properties and past/ongoing/future events
 *
 * TODO By default only the past and present section are open.
 * TODO use the data receive by the organization server
*/
// FIXME refactor when Event storage available
const Attendee = (props: IPropTypes) => {
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
    <>
      <LaoProperties />
      <EventListCollapsible data={DATA_EXAMPLE} />
    </>
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
Attendee.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const mapStateToProps = (state: any) => ({
  events: state.currentEvents.events,
});

export default connect(mapStateToProps)(Attendee);
