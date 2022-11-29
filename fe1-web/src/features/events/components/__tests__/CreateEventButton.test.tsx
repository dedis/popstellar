import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EVENT_FEATURE_IDENTIFIER, EventReactContext } from 'features/events/interface';
import { eventReducer } from 'features/events/reducer';
import { ElectionEventType } from 'features/evoting/components';
import { electionReducer } from 'features/evoting/reducer';
import { MeetingEventType } from 'features/meeting/components';
import { meetingReducer } from 'features/meeting/reducer';
import { RollCallEventType } from 'features/rollCall/components';
import { rollCallReducer } from 'features/rollCall/reducer';
import { walletReducer } from 'features/wallet/reducer';

import CreateEventButton from '../CreateEventButton';

const mockStore = configureStore({
  reducer: combineReducers({
    ...eventReducer,
    ...electionReducer,
    ...meetingReducer,
    ...rollCallReducer,
    ...walletReducer,
  }),
});

const contextValue = {
  [EVENT_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    eventTypes: [ElectionEventType, MeetingEventType, RollCallEventType],
    useIsLaoOrganizer: () => false,
  } as EventReactContext,
};

describe('CreateEventButton', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <MockNavigator component={CreateEventButton} />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});
