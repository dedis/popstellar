import React from 'react';
import { SectionList } from 'react-native';
import PropTypes from 'prop-types';

import PROPS_TYPE from 'res/Props';
import TextBlock from 'components/TextBlock';
import EventGeneral from 'components/eventList/events/EventGeneral';
import LaoProperties from 'components/eventList/LaoProperties';

/**
 * Collapsible list of events: contains 3 lists of events for
 * Past, Present and Future events
 *
 * Nested events should be in the children value of the parent event
*/
const EventListCollapsible = (props: IPropTypes) => {
  const { data } = props;
  const DATA_EXAMPLE = [ // FIXME refactor when Event storage available
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

  const renderItemFn = (
    ({ item }: any) => <EventGeneral event={item} renderItemFn={renderItemFn} />
  );

  return (
    <>
      <LaoProperties />
      <SectionList
        sections={DATA_EXAMPLE}
        keyExtractor={(item, index) => `${item?.object}-${item?.id}-${index}`}
        renderItem={renderItemFn}
        renderSectionHeader={({ section: { title } }) => <TextBlock bold text={title} />}
      />
    </>
  );
};

const propTypes = {
  data: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(PROPS_TYPE.event).isRequired,
  }).isRequired).isRequired,
};
EventListCollapsible.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventListCollapsible;
