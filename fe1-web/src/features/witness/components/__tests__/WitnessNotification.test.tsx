import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import {
  configureTestFeatures,
  mockAddress,
  mockChannel,
  mockKeyPair,
  mockLao,
  mockLaoId,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { addMessages, messageReducer } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Hash, Timestamp } from 'core/objects';
import {
  addNotification,
  discardNotifications,
  markNotificationAsRead,
  notificationReducer,
} from 'features/notification/reducer';
import {
  WITNESS_FEATURE_IDENTIFIER,
  WitnessFeature,
  WitnessReactContext,
} from 'features/witness/interface';
import { MessageToWitnessNotification } from 'features/witness/objects/MessageToWitnessNotification';
import { addMessageToWitness, witnessReducer } from 'features/witness/reducer';

import WitnessNotification from '../WitnessNotification';

const mockMessageId = new Hash('some message id');

// otherwise .fromData won't work
configureTestFeatures();

const timestamp = new Timestamp(1607277600);

// set up mock store
const mockStore = configureStore({
  reducer: combineReducers({
    ...notificationReducer,
    ...witnessReducer,
    ...messageReducer,
  }),
});
const mockNotification = new MessageToWitnessNotification({
  id: 0,
  laoId: mockLaoId,
  title: 'a notification',
  timestamp: new Timestamp(0),
  type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,
  hasBeenRead: false,
  messageId: mockMessageId,
});

const msg = ExtendedMessage.fromMessage(
  ExtendedMessage.fromData(
    {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      name: 'rollcall1',
      description: 'a description',
      id: 'uO7c5qv5zenCd99Q8gLgBg0amZpjUOrmSkN7wAuZ-KM=',
      creation: timestamp,
      location: 'BC410',
      proposed_end: 1718888400,
      proposed_start: 1718884800,
    } as MessageData,
    mockKeyPair,
    mockChannel,
  ),
  mockAddress,
  mockChannel,
);

mockStore.dispatch(addMessages(msg.toState()));
mockStore.dispatch(addMessageToWitness(msg.message_id));
mockStore.dispatch(addNotification(mockNotification.toState()));

const contextValue = {
  [WITNESS_FEATURE_IDENTIFIER]: {
    enabled: true,
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => true,
    addNotification: (notification) => mockStore.dispatch(addNotification(notification)),
    discardNotifications: (args) => mockStore.dispatch(discardNotifications(args)),
    markNotificationAsRead: (args) => mockStore.dispatch(markNotificationAsRead(args)),
    useCurrentLao: () => mockLao,
  } as WitnessReactContext,
};

describe('WitnessNotification', () => {
  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });
});

describe('WitnessNotification actions', () => {
  let dispatchSpy: jest.SpyInstance;

  beforeEach(() => {
    // Spy on the store's dispatch function
    dispatchSpy = jest.spyOn(mockStore, 'dispatch');
  });

  afterEach(() => {
    // Restore the original function after each test
    dispatchSpy.mockRestore();
  });

  it('renders correctly for witnessing', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('on-witness'));
    expect(dispatchSpy).toHaveBeenCalledWith(
      discardNotifications({ laoId: mockLaoId, notificationIds: [mockNotification.id] }),
    );
  });
  it('renders correctly for declining', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('on-decline'));
    expect(dispatchSpy).toHaveBeenCalledWith(
      discardNotifications({ laoId: mockLaoId, notificationIds: [mockNotification.id] }),
    );
  });
});
