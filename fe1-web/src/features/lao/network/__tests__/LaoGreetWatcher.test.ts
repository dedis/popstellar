import { configureStore } from '@reduxjs/toolkit';
import { combineReducers } from 'redux';

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
import { dispatch } from 'core/redux';
import { LaoServer } from 'features/lao/objects/LaoServer';
import {
  addServer,
  addUnhandledGreetLaoMessage,
  greetLaoReducer,
  handleGreetLaoMessage,
  laoReducer,
  serverReducer,
  setCurrentLao,
} from 'features/lao/reducer';
import { WitnessMessage } from 'features/witness/network/messages';

import { makeLaoGreetStoreWatcher, storeBackendAndConnectToPeers } from '../LaoGreetWatcher';
import { GreetLao } from '../messages/GreetLao';

const mockHandleLaoGreetSignature = jest.fn();
const t = new Timestamp(1600000000);
const mockChannel: Channel = `${ROOT_CHANNEL}/${mockLaoId}`;

// setup the test features to enable ExtendedMessage.fromData
configureTestFeatures();

const mockStore = configureStore({
  reducer: combineReducers({
    ...messageReducer,
    ...greetLaoReducer,
    ...laoReducer,
    ...serverReducer,
  }),
});

beforeEach(() => {
  jest.clearAllMocks();
  mockStore.dispatch(clearAllMessages());
});

jest.mock('core/network/NetworkManager.ts');
jest.mock('core/redux', () => {
  const actualModule = jest.requireActual('core/redux');
  return {
    ...actualModule,
    dispatch: jest.fn().mockImplementation(() => {}),
  };
});

describe('handleLaoGreet', () => {
  it('should add the server and mark the greeting message as handled', () => {
    const greetLaoMsg = new GreetLao({
      object: ObjectType.LAO,
      action: ActionType.GREET,
      address: mockAddress,
      lao: mockLaoIdHash,
      peers: [],
      frontend: mockPopToken.publicKey,
    });

    const msg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(greetLaoMsg, mockKeyPair, mockChannel),
      mockAddress,
      mockChannel,
      t,
    );

    storeBackendAndConnectToPeers(msg.message_id, greetLaoMsg, msg.sender);
    expect(dispatch).toHaveBeenCalledTimes(2);
    // the key of the server should have been stored
    expect(dispatch).toHaveBeenCalledWith(
      addServer(
        new LaoServer({
          laoId: greetLaoMsg.lao,
          address: greetLaoMsg.address,
          serverPublicKey: msg.sender,
          frontendPublicKey: greetLaoMsg.frontend,
        }).toState(),
      ),
    );
    // and the greeting message should have been marked as handled
    expect(dispatch).toHaveBeenCalledWith(
      handleGreetLaoMessage({
        messageId: msg.message_id.valueOf(),
      }),
    );
  });
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
        mockChannel,
      ),
      mockAddress,
      mockChannel,
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
        mockChannel,
      ),
      mockAddress,
      mockChannel,
      t,
    );

    const witnessMsg = ExtendedMessage.fromMessage(
      ExtendedMessage.fromData(
        new WitnessMessage({
          message_id: msg.message_id,
          signature: mockKeyPair.privateKey.sign(msg.message_id),
        }),
        mockKeyPair,
        mockChannel,
      ),
      mockAddress,
      mockChannel,
      t,
    );

    mockStore.dispatch(addMessages([msg.toState(), witnessMsg.toState()]));
    watcher();
    expect(mockHandleLaoGreetSignature).toHaveBeenCalledTimes(0);
  });

  it('laoGreetSignatureHandler is called when a new lao#greet message is witnessed by the frontend', () => {
    const watcher = makeLaoGreetStoreWatcher(mockStore, mockHandleLaoGreetSignature);

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
        mockChannel,
      ),
      mockAddress,
      mockChannel,
      t,
    );

    const witnessMessage = new WitnessMessage({
      message_id: greetLaoMessage.message_id,
      // mockKeyPair is the organizer of mockLao
      signature: mockKeyPair.privateKey.sign(greetLaoMessage.message_id),
    });

    const extendedWitnessMessage = ExtendedMessage.fromMessage(
      // mockKeyPair is the organizer of mockLao
      ExtendedMessage.fromData(witnessMessage, mockKeyPair, mockChannel),
      mockAddress,
      mockChannel,
      t,
    );

    const ws = new WitnessSignature({
      witness: extendedWitnessMessage.sender,
      signature: witnessMessage.signature,
    });

    mockStore.dispatch(setCurrentLao(mockLaoState));
    mockStore.dispatch(addMessages([greetLaoMessage.toState(), extendedWitnessMessage.toState()]));
    mockStore.dispatch(
      addUnhandledGreetLaoMessage({
        messageId: greetLaoMessage.message_id.valueOf(),
      }),
    );
    mockStore.dispatch(addMessageWitnessSignature(greetLaoMessage.message_id, ws.toState()));

    watcher();
    expect(mockHandleLaoGreetSignature).toHaveBeenCalledTimes(1);
  });
});
