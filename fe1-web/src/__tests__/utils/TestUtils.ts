import testKeyPair from 'test_data/keypair.json';

import { KeyPairRegistry } from 'core/keypair';
import { JsonRpcMethod, JsonRpcRequest, JsonRpcResponse } from 'core/network/jsonrpc';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Channel, Hash, KeyPair, PopToken, PublicKey, ROOT_CHANNEL, Timestamp } from 'core/objects';
import { Lao, LaoState } from 'features/lao/objects';

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
  subscribed_channels: [],
};
export const mockLao = Lao.fromState(mockLaoState);

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

export const mockChannel: Channel = `${ROOT_CHANNEL}/${mockLaoId}`;
export const mockAddress = 'wss://some-address.com:8000/';

export const mockJsonRequest: Partial<JsonRpcRequest> = {
  jsonrpc: 'some data',
  method: JsonRpcMethod.BROADCAST,
  params: { channel: mockChannel },
};

export const mockJsonResponse: Partial<JsonRpcResponse> = { id: 0, result: [] };
