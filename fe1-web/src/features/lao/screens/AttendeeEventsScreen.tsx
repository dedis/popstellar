import React from 'react';
import { ScrollView } from 'react-native';

import { LaoProperties } from '../components';
import { LaoHooks } from '../hooks';

/**
 * AttendeeScreen: lists LAO properties and past/ongoing/future events.
 * By default, only the past and present section are open.
 */
const AttendeeEventsScreen = () => {
  const EventList = LaoHooks.useEventListComponent();

  return (
    <ScrollView>
      <LaoProperties />
      <EventList />
    </ScrollView>
  );
};

export default AttendeeEventsScreen;
