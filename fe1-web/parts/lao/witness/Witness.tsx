import React from 'react';
import { ScrollView } from 'react-native';

import STRINGS from 'res/strings';
import { useNavigation } from '@react-navigation/native';
import { useSelector } from 'react-redux';
import LaoProperties from 'components/eventList/LaoProperties';
import EventListCollapsible from 'components/eventList/EventListCollapsible';
import TextBlock from 'components/TextBlock';
import WideButtonView from 'components/WideButtonView';
import { makeEventsList } from 'store/reducers';

const laoToProperties = (events: any) => [[], ...events];

/**
 * Witness screen: button to navigate to the witness video screen,
 * a section list of events and lao properties
*/
const Witness = () => {
  const navigation = useNavigation();

  const eventList = makeEventsList();
  const events = useSelector(eventList);

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

export default Witness;
