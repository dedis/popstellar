import { configureStore } from '@reduxjs/toolkit';
import { combineReducers } from 'redux';

import {
  configureTestFeatures,
  mockAddress,
  mockChannel,
  mockKeyPair,
  mockLaoId,
  mockChannel2,
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

const mockStore = configureStore({ reducer: combineReducers(messageReducer) });

beforeEach(() => {
  jest.clearAllMocks();
  mockStore.dispatch(clearAllMessages());
});

describe('makeWitnessStoreWatcher', () => {
  it('returns a listener function', () => {
    expect(
      makeWitnessStoreWatcher(mockStore, () => mockLaoId, mockAfterProcessingHandler),
    ).toBeFunction();
  });

  it('afterProcessingHandler is not called when a new message is added', () => {
    const watcher = makeWitnessStoreWatcher(mockStore, () => mockLaoId, mockAfterProcessingHandler);

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp: t,
        } as AddChirp,
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
    const watcher = makeWitnessStoreWatcher(mockStore, () => mockLaoId, mockAfterProcessingHandler);

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp: t,
        } as AddChirp,
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
  it('calls afterProcessingHandler only on the message of this lao', () => {
    const watcher = makeWitnessStoreWatcher(mockStore, () => mockLaoId, mockAfterProcessingHandler);
    const msg1 = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hi',
          timestamp: t,
        } as AddChirp,
        mockKeyPair,
        mockChannel,
      ),
      mockAddress,
      mockChannel,
      t,
    );

    const msg2 = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        {
          object: ObjectType.CHIRP,
          action: ActionType.ADD,
          text: 'hello',
          timestamp: t,
        } as AddChirp,
        mockKeyPair,
        mockChannel2,
      ),
      mockAddress,
      mockChannel2,
      t,
    );
    mockStore.subscribe(watcher);
    mockStore.dispatch(addMessages([msg1.toState()]));
    mockStore.dispatch(addMessages([msg2.toState()]));
    mockStore.dispatch(processMessages([msg1.message_id]));
    mockStore.dispatch(processMessages([msg2.message_id]));

    expect(msg1.message_id).not.toBe(msg2.message_id);
    expect(mockAfterProcessingHandler).toHaveBeenCalledTimes(1);
    expect(mockAfterProcessingHandler).toHaveBeenCalledWith(
      expect.objectContaining({
        message_id: msg1.message_id,
      }),
    );
    expect(mockAfterProcessingHandler).not.toHaveBeenCalledWith(
      expect.objectContaining({
        message_id: msg2.message_id,
      }),
    );
  });
});
