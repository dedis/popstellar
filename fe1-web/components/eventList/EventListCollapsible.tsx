import React from 'react';
import { SectionList } from 'react-native';
import PropTypes from 'prop-types';

import { Event as EventObj } from 'model/objects';

import TextBlock from 'components/TextBlock';
import Event from './events';

/**
 * Collapsible list of events: contains 3 lists of events for
 * Past, Present and Future events
 *
 * Nested events should be in the children value of the parent event
*/
const EventListCollapsible = (props: IPropTypes) => {
  const { data } = props;

  const renderItemFn = (
    ({ item }: any) => <Event event={item} renderItemFn={renderItemFn} />
  );

  return (
    <SectionList
      sections={data}
      keyExtractor={(item, index) => `${item?.object}-${item?.id}-${index}`}
      renderItem={renderItemFn}
      renderSectionHeader={({ section: { title } }) => <TextBlock bold text={title} />}
    />
  );
};

const propTypes = {
  data: PropTypes.arrayOf(PropTypes.shape({
    title: PropTypes.string.isRequired,
    data: PropTypes.arrayOf(PropTypes.instanceOf(EventObj)).isRequired,
  }).isRequired).isRequired,
};
EventListCollapsible.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

export default EventListCollapsible;
