import { ListItem } from '@rneui/themed';
import PropTypes from 'prop-types';
import React, { useState } from 'react';

import { List, Typography } from 'core/styles';

import { eventStatePropType } from '../objects';
import EventListItem from './EventListItem';

const EventList = ({ title, isDefaultExpanded, toggleable, events }: IPropTypes) => {
  const [showItems, setShowItems] = useState(!toggleable || isDefaultExpanded === true);

  if (events.length <= 0) {
    return null;
  }

  return (
    <ListItem.Accordion
      containerStyle={List.accordionItem}
      style={List.accordionItem}
      content={
        <ListItem.Content>
          <ListItem.Title style={[Typography.base, Typography.important]}>{title}</ListItem.Title>
        </ListItem.Content>
      }
      isExpanded={showItems}
      onPress={toggleable ? () => setShowItems(!showItems) : undefined}>
      {events.map((event, idx) => (
        <EventListItem
          key={event.id}
          eventId={event.id}
          eventType={event.eventType}
          isFirstItem={idx === 0}
          isLastItem={idx === events.length - 1}
          testID={`current_event_selector_${idx}`}
        />
      ))}
    </ListItem.Accordion>
  );
};

const propTypes = {
  title: PropTypes.string.isRequired,
  events: PropTypes.arrayOf(eventStatePropType).isRequired,
  isDefaultExpanded: PropTypes.bool,
  toggleable: PropTypes.bool,
};
EventList.propTypes = propTypes;

EventList.defaultProps = {
  isDefaultExpanded: false,
  toggleable: true,
};

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventList;
