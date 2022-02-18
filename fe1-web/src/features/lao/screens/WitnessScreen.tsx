import React from 'react';
import { ScrollView } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { useSelector } from 'react-redux';

import STRINGS from 'res/strings';
import { TextBlock, WideButtonView } from 'core/components';
import { Timestamp } from 'model/objects';
import EventListCollapsible from 'features/events/components/EventListCollapsible';
import { LaoEvent } from 'features/events/objects';
import { makeEventsList } from 'features/events/reducer';

import LaoProperties from 'features/lao/components/LaoProperties';

/**
 * WitnessScreen: button to navigate to the witness video screen,
 * a section list of events and lao properties
 */
const WitnessScreen = () => {
  // FIXME: Navigation should use a defined type here (instead of any)
  const navigation = useNavigation<any>();

  const eventList = makeEventsList();
  const events = useSelector(eventList);

  const now = Timestamp.EpochNow();
  const pastEvents: LaoEvent[] = [];
  const currentEvents: LaoEvent[] = [];
  const futureEvents: LaoEvent[] = [];

  events.forEach((e: LaoEvent) => {
    if (new Timestamp(e.end).before(now)) {
      pastEvents.push(e);
      return;
    }
    if (new Timestamp(e.start).after(now)) {
      futureEvents.push(e);
      return;
    }
    currentEvents.push(e);
  });
  // TODO: nesting logic

  const DATA_EXAMPLE = [
    {
      title: 'Past',
      data: pastEvents,
    },
    {
      title: 'Present',
      data: currentEvents,
    },
    {
      title: 'Future',
      data: futureEvents,
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

export default WitnessScreen;
