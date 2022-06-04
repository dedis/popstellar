import { combineReducers, createStore } from 'redux';

import {
  configureTestFeatures,
  mockAddress,
  mockChannel,
  mockKeyPair,
  mockLaoIdHash,
} from '__tests__/utils';
import {
  addMessages,
  clearAllMessages,
  messageReducer,
  processMessages,
} from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Timestamp } from 'core/objects';
import { AddChirp } from 'features/social/network/messages/chirp';

import { makeWitnessStoreWatcher } from '../WitnessStoreWatcher';

const mockAfterProcessingHandler = jest.fn();

const t = new Timestamp(1600000000);

// setup the test features to enable ExtendedMessage.fromData
configureTestFeatures();

const mockStore = createStore(combineReducers(messageReducer));

beforeEach(() => {
  jest.clearAllMocks();
  mockStore.dispatch(clearAllMessages());
});

describe('makeWitnessStoreWatcher', () => {
  it('returns a listener function', () => {
    expect(
      makeWitnessStoreWatcher(mockStore, () => mockLaoIdHash, mockAfterProcessingHandler),
    ).toBeFunction();
  });

  it('afterProcessingHandler is not called when a new message is added', () => {
    const watcher = makeWitnessStoreWatcher(
      mockStore,
      () => mockLaoIdHash,
      mockAfterProcessingHandler,
    );

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        { object: ObjectType.CHIRP, action: ActionType.ADD, text: 'hi', timestamp: t } as AddChirp,
        mockKeyPair,
        mockChannel,
      ),
      mockAddress,
      mockChannel,
      t,
    );

    mockStore.dispatch(addMessages([msg.toState()]));
    watcher();
    expect(mockAfterProcessingHandler).toHaveBeenCalledTimes(0);
  });

  it('afterProcessingHandler is called when a message has been processed', () => {
    const watcher = makeWitnessStoreWatcher(
      mockStore,
      () => mockLaoIdHash,
      mockAfterProcessingHandler,
    );

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        { object: ObjectType.CHIRP, action: ActionType.ADD, text: 'hi', timestamp: t } as AddChirp,
        mockKeyPair,
        mockChannel,
      ),
      mockAddress,
      mockChannel,
      t,
    );

    mockStore.dispatch(addMessages([msg.toState()]));
    watcher();
    mockStore.dispatch(processMessages([msg.message_id]));
    watcher();

    expect(mockAfterProcessingHandler).toHaveBeenCalledTimes(1);
    expect(mockAfterProcessingHandler).toHaveBeenCalledWith(
      expect.objectContaining({
        message_id: msg.message_id,
      }),
    );
  });
});
