import { combineReducers, createStore } from 'redux';

import {
  configureTestFeatures,
  mockAddress,
  mockKeyPair,
  mockLaoId,
  mockLaoIdHash,
  mockLaoState,
  mockPopToken,
} from '__tests__/utils';
import {
  addMessages,
  addMessageWitnessSignature,
  clearAllMessages,
  messageReducer,
} from 'core/network/ingestion';
import { ExtendedMessage } from 'core/network/ingestion/ExtendedMessage';
import { ActionType, ObjectType } from 'core/network/jsonrpc/messages';
import { Channel, ROOT_CHANNEL, Timestamp, WitnessSignature } from 'core/objects';
import {
  addGreetLaoMessage,
  connectToLao,
  greetLaoReducer,
  laoReducer,
  serverReducer,
} from 'features/lao/reducer';
import { WitnessMessage } from 'features/witness/network/messages';

import { makeLaoGreetStoreWatcher } from '../LaoGreetWatcher';
import { GreetLao } from '../messages/GreetLao';

const mockHandleLaoGreetSignature = jest.fn();
const t = new Timestamp(1600000000);
const mockChannel: Channel = `${ROOT_CHANNEL}/${mockLaoId}`;

// setup the test features to enable ExtendedMessage.fromData
configureTestFeatures();

const mockStore = createStore(
  combineReducers({ ...messageReducer, ...greetLaoReducer, ...laoReducer, ...serverReducer }),
);

beforeEach(() => {
  jest.clearAllMocks();
  mockStore.dispatch(clearAllMessages());
});

describe('makeLaoGreetStoreWatcher', () => {
  it('returns a listener function', () => {
    expect(makeLaoGreetStoreWatcher(mockStore, mockHandleLaoGreetSignature)).toBeFunction();
  });

  it('laoGreetSignatureHandler is not called when a new lao#greet message is added', () => {
    const watcher = makeLaoGreetStoreWatcher(
      mockStore,

      mockHandleLaoGreetSignature,
    );

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
    const watcher = makeLaoGreetStoreWatcher(
      mockStore,

      mockHandleLaoGreetSignature,
    );

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
    const watcher = makeLaoGreetStoreWatcher(
      mockStore,

      mockHandleLaoGreetSignature,
    );

    const greetLaoMessage = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new GreetLao({
          object: ObjectType.LAO,
          action: ActionType.GREET,
          address: mockAddress,
          lao: mockLaoIdHash,
          peers: [],
          frontend: mockPopToken.publicKey,
        }),
        mockPopToken,
      ),
      mockChannel,
      mockAddress,
      t,
    );

    const witnessMessage = new WitnessMessage({
      message_id: greetLaoMessage.message_id,
      // mockKeyPair is the organizer of mockLao
      signature: mockKeyPair.privateKey.sign(greetLaoMessage.message_id),
    });

    const extendedWitnessMessage = ExtendedMessage.fromMessage(
      // mockKeyPair is the organizer of mockLao
      ExtendedMessage.fromData(witnessMessage, mockKeyPair),
      mockChannel,
      mockAddress,
      t,
    );

    const ws = new WitnessSignature({
      witness: extendedWitnessMessage.sender,
      signature: witnessMessage.signature,
    });

    mockStore.dispatch(connectToLao(mockLaoState));
    mockStore.dispatch(addMessages([greetLaoMessage.toState(), extendedWitnessMessage.toState()]));
    mockStore.dispatch(
      addGreetLaoMessage({ laoId: mockLaoId, messageId: greetLaoMessage.message_id.valueOf() }),
    );
    mockStore.dispatch(addMessageWitnessSignature(greetLaoMessage.message_id, ws.toState()));

    watcher();
    expect(mockHandleLaoGreetSignature).toHaveBeenCalledTimes(1);
  });
});
