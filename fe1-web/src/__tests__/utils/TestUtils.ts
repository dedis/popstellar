import testKeyPair from 'test_data/keypair.json';

import { KeyPairRegistry } from 'core/keypair';
import { JsonRpcMethod, JsonRpcRequest, JsonRpcResponse } from 'core/network/jsonrpc';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Channel, EventTags, Hash, KeyPair, PopToken, PublicKey, Timestamp } from 'core/objects';
import { Lao, LaoState } from 'features/lao/objects';
import { EventTypeRollCall, RollCall, RollCallStatus } from 'features/rollCall/objects';

export const mockPublicKey = testKeyPair.publicKey;
export const mockPrivateKey = testKeyPair.privateKey;
export const mockKeyPair = KeyPair.fromState({
  publicKey: mockPublicKey,
  privateKey: mockPrivateKey,
});

export const mockPublicKey2 = testKeyPair.publicKey2;
export const mockPrivateKey2 = testKeyPair.privateKey2;

export const mockPopToken = PopToken.fromState({
  publicKey: testKeyPair.publicKey2,
  privateKey: testKeyPair.privateKey2,
});

export const org = new PublicKey(mockPublicKey);

// MOCK LAO
export const mockLaoName = 'MyLao';
export const mockLaoCreationTime = new Timestamp(1600000000);
export const mockLaoIdHash: Hash = Hash.fromStringArray(
  org.toString(),
  mockLaoCreationTime.toString(),
  mockLaoName,
);
export const mockLaoId: string = mockLaoIdHash.toString();

export const mockLaoState: LaoState = {
  id: mockLaoId,
  name: mockLaoName,
  creation: mockLaoCreationTime.valueOf(),
  last_modified: mockLaoCreationTime.valueOf(),
  organizer: org.valueOf(),
  witnesses: [],
  server_addresses: [],
};
export const mockLao = Lao.fromState(mockLaoState);

// MOCK ROLL CALL
const mockRCName = 'myRollCall';
const mockRCLocation = 'location';
const mockRCTimestampStart = new Timestamp(1620255600);
const mockRCTimestampEnd = new Timestamp(1620357600);
const mockRCAttendees = ['attendee1', 'attendee2'];

const mockRCIdHash = Hash.fromStringArray(
  EventTags.ROLL_CALL,
  mockLaoId,
  mockRCTimestampStart.toString(),
  mockRCName,
);

export const mockRollCallState: any = {
  id: mockRCIdHash.valueOf(),
  eventType: EventTypeRollCall,
  start: mockRCTimestampStart.valueOf(),
  name: mockRCName,
  location: mockRCLocation,
  creation: mockRCTimestampStart.valueOf(),
  proposedStart: mockRCTimestampStart.valueOf(),
  proposedEnd: mockRCTimestampEnd.valueOf(),
  status: RollCallStatus.CLOSED,
  attendees: mockRCAttendees,
};
export const mockRC = RollCall.fromState(mockRollCallState);

export const defaultMessageDataFields = ['object', 'action'];

export const mockReduxAction = {
  type: '',
  payload: undefined,
};

export const messageRegistryInstance = new MessageRegistry();

export const mockSignatureType = 'some signature';

export const mockMessageRegistry = {
  getSignatureType: jest.fn(() => mockSignatureType),
  buildMessageData: jest.fn((input) => JSON.stringify(input)),
} as unknown as MessageRegistry;

export const mockKeyPairRegistry = {
  getSignatureKeyPair: jest.fn(() => Promise.resolve(mockKeyPair)),
} as unknown as KeyPairRegistry;

export const mockChannel: Channel = 'some channel';
export const mockAddress = 'wss://some-address.com';

export const mockJsonRequest: Partial<JsonRpcRequest> = {
  jsonrpc: 'some data',
  method: JsonRpcMethod.BROADCAST,
  params: { channel: mockChannel },
};

export const mockJsonResponse: Partial<JsonRpcResponse> = { id: 0, result: [] };
