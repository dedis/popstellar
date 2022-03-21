import React from 'react';
import { ScrollView } from 'react-native';

import { LaoProperties } from '../components';
import { LaoHooks } from '../hooks';

/**
 * AttendeeScreen: lists LAO properties and past/ongoing/future events.
 * By default, only the past and present section are open.
 *
 * TODO: use the data receive by the organization server
 */
const AttendeeScreen = () => {
  const EventList = LaoHooks.useEventList();

  return (
    <ScrollView>
      <LaoProperties />
      <EventList />
    </ScrollView>
  );
};

export default AttendeeScreen;
