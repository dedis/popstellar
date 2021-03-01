import React from 'react';
import { ScrollView } from 'react-native';

import STRINGS from 'res/strings';
import PROPS_TYPE from 'res/Props';
import { useNavigation } from '@react-navigation/native';
import PropTypes from 'prop-types';
import { connect } from 'react-redux';
import LaoProperties from 'components/eventList/LaoProperties';
import EventListCollapsible from 'components/eventList/EventListCollapsible';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';

const laoToProperties = (events: any) => [[], ...events];

/**
 * Witness screen: button to navigate to the witness video screen,
 * a section list of events and lao properties
*/
const Witness = (props: IPropTypes) => {
  const { events } = props;
  const navigation = useNavigation();

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
      <TextBlock bold text="Witness Panel" />
      <WideButtonView
        title={STRINGS.witness_video_button}
        onPress={() => navigation.navigate(STRINGS.witness_navigation_tab_video)}
      />
      <LaoProperties />
      <EventListCollapsible data={DATA_EXAMPLE} />
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
Witness.propTypes = propTypes;

type IPropTypes = PropTypes.InferProps<typeof propTypes>;

const mapStateToProps = (state: any) => ({
  events: state.currentEvents.events,
});

export default connect(mapStateToProps)(Witness);
