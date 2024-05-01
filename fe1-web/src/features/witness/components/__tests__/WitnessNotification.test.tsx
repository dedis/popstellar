import { configureStore } from '@reduxjs/toolkit';
import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { Store, combineReducers } from 'redux';

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

interface ContextValue {
  [identifier: string]: unknown;
}

// otherwise .fromData won't work
configureTestFeatures();

const timestamp = new Timestamp(1607277600);

const msg0 = ExtendedMessage.fromMessage(
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

const mockNotification0 = new MessageToWitnessNotification({
  id: 0,
  laoId: mockLaoId,
  title: 'a notification',
  timestamp: new Timestamp(0),
  type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,
  hasBeenRead: false,
  messageId: msg0.message_id,
});

const msg1 = ExtendedMessage.fromMessage(
  ExtendedMessage.fromData(
    {
      object: ObjectType.ROLL_CALL,
      action: ActionType.CREATE,
      name: 'rollcall2',
      id: 'hA4d8e-lYmd4u5_Ltv5Lft_P8Q_YRMXI1UmikEYD8vc=',
      creation: timestamp,
      location: 'BC411',
      proposed_end: 1714385635,
      proposed_start: 1714378435,
    } as MessageData,
    mockKeyPair,
    mockChannel,
  ),
  mockAddress,
  mockChannel,
);

const mockNotification1 = new MessageToWitnessNotification({
  id: 1,
  laoId: mockLaoId,
  title: 'a notification',
  timestamp: new Timestamp(0),
  type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,
  hasBeenRead: false,
  messageId: msg1.message_id,
});

const mockNotification2 = new MessageToWitnessNotification({
  id: 2,
  laoId: mockLaoId,
  title: 'a notification',
  timestamp: new Timestamp(0),
  type: WitnessFeature.NotificationTypes.MESSAGE_TO_WITNESS,
  hasBeenRead: false,
  messageId: new Hash('some message id3'),
});

const setupStore = (): Store => {
  // set up mock store
  const mockStore = configureStore({
    reducer: combineReducers({
      ...notificationReducer,
      ...witnessReducer,
      ...messageReducer,
    }),
  });

  mockStore.dispatch(addMessages(msg0.toState()));
  mockStore.dispatch(addMessageToWitness(msg0.message_id));
  mockStore.dispatch(addNotification(mockNotification0.toState()));

  mockStore.dispatch(addMessages(msg1.toState()));
  mockStore.dispatch(addMessageToWitness(msg1.message_id));
  mockStore.dispatch(addNotification(mockNotification1.toState()));

  mockStore.dispatch(addNotification(mockNotification2.toState()));

  return mockStore;
};

const getContextValue = (store: Store): ContextValue => {
  const contextValue = {
    [WITNESS_FEATURE_IDENTIFIER]: {
      enabled: true,
      useCurrentLaoId: () => mockLaoId,
      useConnectedToLao: () => true,
      addNotification: (notification) => store.dispatch(addNotification(notification)),
      discardNotifications: (args) => store.dispatch(discardNotifications(args)),
      markNotificationAsRead: (args) => store.dispatch(markNotificationAsRead(args)),
      useCurrentLao: () => mockLao,
    } as WitnessReactContext,
  };

  return contextValue;
};

let mockDate: jest.SpyInstance;

beforeAll(() => {
  mockDate = jest.spyOn(Date.prototype, 'toLocaleString').mockReturnValue('2020-01-01');
});

afterAll(() => {
  mockDate.mockRestore();
});

describe('WitnessNotification', () => {
  let mockStore: Store;
  let contextValue: ContextValue;

  beforeEach(() => {
    mockStore = setupStore();
    contextValue = getContextValue(mockStore);
  });

  it('renders correctly', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification0}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly without description', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification1}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    ).toJSON();
    expect(component).toMatchSnapshot();
  });

  it('renders correctly 3', () => {
    const component = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification2}
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
  let mockStore: Store;
  let contextValue: ContextValue;

  beforeEach(() => {
    mockStore = setupStore();
    contextValue = getContextValue(mockStore);

    // Spy on the store's dispatch function
    dispatchSpy = jest.spyOn(mockStore, 'dispatch');
  });

  afterEach(() => {
    // Restore the original function after each test
    dispatchSpy.mockRestore();
  });

  it('renders correctly for witnessing not1', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification0}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('on-witness'));
    expect(dispatchSpy).toHaveBeenCalledWith(
      discardNotifications({ laoId: mockLaoId, notificationIds: [mockNotification0.id] }),
    );
  });
  it('renders correctly for declining not1', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification0}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('on-decline'));
    expect(dispatchSpy).toHaveBeenCalledWith(
      markNotificationAsRead({ laoId: mockLaoId, notificationId: mockNotification0.id }),
    );
  });

  it('renders correctly for witnessing not2', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification1}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('on-witness'));
    expect(dispatchSpy).toHaveBeenCalledWith(
      discardNotifications({ laoId: mockLaoId, notificationIds: [mockNotification1.id] }),
    );
  });
  it('renders correctly for declining not2', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification1}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('on-decline'));
    expect(dispatchSpy).toHaveBeenCalledWith(
      markNotificationAsRead({ laoId: mockLaoId, notificationId: mockNotification1.id }),
    );
  });

  it('renders correctly for witnessing not3', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification2}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    fireEvent.press(getByTestId('on-witness'));
    expect(dispatchSpy).toHaveBeenCalledWith(
      discardNotifications({ laoId: mockLaoId, notificationIds: [mockNotification2.id] }),
    );
  });
  it('renders correctly for declining not3', () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <FeatureContext.Provider value={contextValue}>
          <WitnessNotification
            notification={mockNotification2}
            navigateToNotificationScreen={() => {}}
          />
        </FeatureContext.Provider>
      </Provider>,
    );

    // if there is no associated message, on-decline should be a no-op
    const calls = dispatchSpy.mock.calls.length;
    fireEvent.press(getByTestId('on-decline'));
    expect(dispatchSpy.mock.calls.length).toEqual(calls);
  });
});
