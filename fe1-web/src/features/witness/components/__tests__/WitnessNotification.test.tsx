import { render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import {
  configureTestFeatures,
  mockKeyPair,
  mockLao,
  mockLaoId,
  mockLaoIdHash,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { Timestamp } from 'core/objects';
import {
  addNotification,
  discardNotifications,
  markNotificationAsRead,
  notificationReducer,
} from 'features/notification/reducer';
import {
  MESSAGE_TO_WITNESS_NOTIFICATION_TYPE,
  WintessReactContext,
  WitnessFeature,
  WITNESS_FEATURE_IDENTIFIER,
} from 'features/witness/interface';
import { addMessageToWitness, witnessReducer } from 'features/witness/reducer';

import WitnessNotification from '../WitnessNotification';

const mockMessageId = 'some message id';

// otherwise .formData won't work
configureTestFeatures();

const timestamp = new Timestamp(1607277600);

// set up mock store
const mockStore = createStore(combineReducers({ ...notificationReducer, ...witnessReducer }));
const mockNotification = {
  id: 0,
  laoId: mockLaoId,
  title: 'a notification',
  timestamp: 0,
  type: MESSAGE_TO_WITNESS_NOTIFICATION_TYPE,
  hasBeenRead: false,
  messageId: mockMessageId,
} as WitnessFeature.MessageToWitnessNotification;

mockStore.dispatch(
  addMessageToWitness(
    ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        { object: ObjectType.CHIRP, action: ActionType.ADD, text: 'hi', timestamp } as MessageData,
        mockKeyPair,
      ),
      'some channel',
      'some address',
    ).toState(),
  ),
);
mockStore.dispatch(addNotification(mockNotification));

const contextValue = {
  [WITNESS_FEATURE_IDENTIFIER]: {
    enabled: true,
    useCurrentLaoId: () => mockLaoIdHash,
    addNotification: (notification) => mockStore.dispatch(addNotification(notification)),
    discardNotifications: (args) => mockStore.dispatch(discardNotifications(args)),
    markNotificationAsRead: (args) => mockStore.dispatch(markNotificationAsRead(args)),
    useCurrentLao: () => mockLao,
  } as WintessReactContext,
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
