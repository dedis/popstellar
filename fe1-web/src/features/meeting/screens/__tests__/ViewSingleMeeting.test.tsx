import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { FIXED_SYSTEM_TIME, mockLaoId, mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import EventReducer, { addEvent } from 'features/events/reducer/EventReducer';
import { mockMeeting } from 'features/meeting/__tests__/utils';
import { MeetingReactContext, MEETING_FEATURE_IDENTIFIER } from 'features/meeting/interface';
import { addMeeting, meetingReducer } from 'features/meeting/reducer';

import { Meeting } from '../../objects';
import ViewSingleMeeting, { ViewSingleMeetingScreenHeader } from '../ViewSingleMeeting';

jest.useFakeTimers('modern');
jest.setSystemTime(FIXED_SYSTEM_TIME);

const mockStore = createStore(combineReducers({ ...EventReducer, ...meetingReducer }));
mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: Meeting.EVENT_TYPE,
    id: mockMeeting.id.valueOf(),
    start: mockMeeting.start.valueOf(),
    end: mockMeeting.end?.valueOf(),
  }),
);
mockStore.dispatch(addMeeting(mockMeeting.toState()));

const contextValue = {
  [MEETING_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
  } as MeetingReactContext,
};

describe('ViewSingleMeeting', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ViewSingleMeeting}
            params={{ eventId: mockMeeting.id.valueOf(), isOrganizer: false }}
          />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});

describe('ViewSingleMeetingScreenHeader', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator
            component={ViewSingleMeetingScreenHeader}
            params={{ eventId: mockMeeting.id.valueOf(), isOrganizer: false }}
          />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
