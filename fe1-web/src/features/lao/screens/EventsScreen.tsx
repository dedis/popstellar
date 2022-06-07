import React from 'react';

import ScreenWrapper from 'core/components/ScreenWrapper';

import { LaoHooks } from '../hooks';

/**
 * AttendeeScreen: lists LAO properties and past/ongoing/future events.
 * By default, only the past and present section are open.
 */
const EventsScreen = () => {
  const EventList = LaoHooks.useEventListComponent();

  return (
    <ScreenWrapper>
      <EventList />
    </ScreenWrapper>
  );
};

export default EventsScreen;
