import { combineReducers, createStore } from 'redux';

import {
  configureTestFeatures,
  mockAddress,
  mockChannel,
  mockKeyPair,
  mockLaoIdHash,
  mockPopToken,
} from '__tests__/utils';
import { addMessages, clearAllMessages, messageReducer } from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Timestamp } from 'core/objects';
import { WitnessMessage } from 'features/witness/network/messages';

import { makeLaoGreetStoreWatcher } from '../LaoGreetWatcher';
import { GreetLao } from '../messages/GreetLao';

const mockHandleLaoGreetSignature = jest.fn();
const t = new Timestamp(1600000000);

// setup the test features to enable ExtendedMessage.fromData
configureTestFeatures();

const mockStore = createStore(combineReducers(messageReducer));

beforeEach(() => {
  jest.clearAllMocks();
  mockStore.dispatch(clearAllMessages());
});

describe('makeLaoGreetStoreWatcher', () => {
  it('returns a listener function', () => {
    expect(makeLaoGreetStoreWatcher(mockStore, mockHandleLaoGreetSignature)).toBeFunction();
  });

  it('laoGreetSignatureHandler is not called when a new lao#greet message is added', () => {
    const watcher = makeLaoGreetStoreWatcher(mockStore, mockHandleLaoGreetSignature);

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new GreetLao({
          object: ObjectType.LAO,
          action: ActionType.GREET,
          address: mockAddress,
          lao: mockLaoIdHash,
          peers: [],
          frontend: mockPopToken.publicKey,
        }),
        mockKeyPair,
      ),
      mockChannel,
      mockAddress,
      t,
    );

    mockStore.dispatch(addMessages([msg.toState()]));
    watcher();
    expect(mockHandleLaoGreetSignature).toHaveBeenCalledTimes(0);
  });

  it('laoGreetSignatureHandler is not called when a new lao#greet message is witnessed by somebody other than the frontend', () => {
    const watcher = makeLaoGreetStoreWatcher(mockStore, mockHandleLaoGreetSignature);

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new GreetLao({
          object: ObjectType.LAO,
          action: ActionType.GREET,
          address: mockAddress,
          lao: mockLaoIdHash,
          peers: [],
          frontend: mockPopToken.publicKey,
        }),
        mockKeyPair,
      ),
      mockChannel,
      mockAddress,
      t,
    );

    const witnessMsg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new WitnessMessage({
          message_id: msg.message_id,
          signature: mockKeyPair.privateKey.sign(msg.message_id),
        }),
        mockKeyPair,
      ),
      mockChannel,
      mockAddress,
      t,
    );

    mockStore.dispatch(addMessages([msg.toState(), witnessMsg.toState()]));
    watcher();
    expect(mockHandleLaoGreetSignature).toHaveBeenCalledTimes(0);
  });

  it('laoGreetSignatureHandler is called when a new lao#greet message is witnessed by the frontend', () => {
    const watcher = makeLaoGreetStoreWatcher(mockStore, mockHandleLaoGreetSignature);

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new GreetLao({
          object: ObjectType.LAO,
          action: ActionType.GREET,
          address: mockAddress,
          lao: mockLaoIdHash,
          peers: [],
          frontend: mockPopToken.publicKey,
        }),
        mockKeyPair,
      ),
      mockChannel,
      mockAddress,
      t,
    );

    const witnessMsg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new WitnessMessage({
          message_id: msg.message_id,
          signature: mockPopToken.privateKey.sign(msg.message_id),
        }),
        mockPopToken,
      ),
      mockChannel,
      mockAddress,
      t,
    );

    mockStore.dispatch(addMessages([msg.toState(), witnessMsg.toState()]));
    watcher();
    expect(mockHandleLaoGreetSignature).toHaveBeenCalledTimes(1);
  });
});
