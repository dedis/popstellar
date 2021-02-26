import React from 'react';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';

import PROPS_TYPE from 'res/Props';
import EventListCollapsible from 'components/eventList/EventListCollapsible';

/**
 * Attendee screen: lists LAO properties and past/ongoing/future events
 *
 * TODO By default only the past and present section are open.
 * TODO use the data receive by the organization server
*/
// FIXME refactor when Event storage available
const laoToProperties = (events: any) => [[], ...events];

const propTypes = {
  events: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(
      PropTypes.oneOfType([PROPS_TYPE.event, PROPS_TYPE.property]),
    ).isRequired,
  })).isRequired,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const Attendee = (props: IPropTypes) => {
  const { events } = props;

  return <EventListCollapsible data={laoToProperties(events)} />;
};

Attendee.propTypes = propTypes;

const mapStateToProps = (state: any) => ({
  events: state.currentEvents.events,
});

export default connect(mapStateToProps)(Attendee);
