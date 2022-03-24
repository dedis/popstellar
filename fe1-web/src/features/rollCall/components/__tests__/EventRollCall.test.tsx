import { describe, it } from '@jest/globals';
import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { configureTestFeatures, mockLao } from '__tests__/utils';
import { Hash, Timestamp } from 'core/objects';
import { eventsReducer } from 'features/events/reducer';
import { connectToLao, laoReducer } from 'features/lao/reducer';
import { RollCall, RollCallStatus } from 'features/rollCall/objects';

import EventRollCall from '../EventRollCall';

const ROLLCALL_CREATION = new Timestamp(1607277600);
const ROLLCALL_START = ROLLCALL_CREATION;
const ROLLCALL_END = new Timestamp(1607277600 + 1000 * 60 * 60);

const mockRollCallEvent = new RollCall({
  id: new Hash('some id'),
  name: 'a roll call',
  location: 'somewhere',
  creation: ROLLCALL_CREATION,
  proposedStart: ROLLCALL_START,
  proposedEnd: ROLLCALL_END,
  status: RollCallStatus.OPENED,
  attendees: [],
});

beforeAll(() => {
  // the wallet uses the global store hence the full test features are required
  configureTestFeatures();
});

// set up mock store
const mockStore = createStore(combineReducers(laoReducer));
mockStore.dispatch(connectToLao(mockLao.toState()));

describe('EventRollCall', () => {
  it('renders correctly as organizer', () => {
    const Screen = () => <EventRollCall event={mockRollCallEvent} isOrganizer />;

    const obj = render(
      <Provider store={mockStore}>
        <MockNavigator component={Screen} />
      </Provider>,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });

  it('renders correctly as non-organizer', () => {
    const Screen = () => <EventRollCall event={mockRollCallEvent} />;

    const obj = render(
      <Provider store={mockStore}>
        <MockNavigator component={Screen} />
      </Provider>,
    );
    expect(obj.toJSON()).toMatchSnapshot();
  });
});
