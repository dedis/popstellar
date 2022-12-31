import { configureStore } from '@reduxjs/toolkit';
import { render } from '@testing-library/react-native';
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
      object: ObjectType.CHIRP,
      action: ActionType.ADD,
      text: 'hi',
      timestamp,
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
