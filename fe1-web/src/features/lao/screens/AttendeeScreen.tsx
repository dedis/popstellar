import { useRoute } from '@react-navigation/core';
import React, { useState } from 'react';
import { ScrollView } from 'react-native';
import { useSelector } from 'react-redux';

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

  // FIXME: route should use proper type
  const route = useRoute<any>();
  const { url } = route.params || '';
  const [serverUrl] = useState(url);

  return (
    <ScrollView>
      <LaoProperties url={serverUrl} />
      <EventList />
    </ScrollView>
  );
};

export default AttendeeScreen;
