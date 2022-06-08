import testKeyPair from 'test_data/keypair.json';

import { KeyPairRegistry } from 'core/keypair';
import { JsonRpcMethod, JsonRpcRequest, JsonRpcResponse } from 'core/network/jsonrpc';
import { MessageRegistry } from 'core/network/jsonrpc/messages';
import { Channel, Hash, KeyPair, PopToken, PublicKey, ROOT_CHANNEL, Timestamp } from 'core/objects';
import { Lao, LaoState } from 'features/lao/objects';
import { COINBASE_HASH, SCRIPT_TYPE } from 'resources/const';

import { TransactionJSON, TransactionState } from '../../features/digital-cash/objects/transaction';

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

// MOCK Transactions

export const mockTransactionValue = 100;
export const mockKPHash = Hash.fromPublicKey(mockKeyPair.publicKey);
export const mockCBSig =
  'ts5vHgbiGPu55Acj1Mo72kypWYfMZCs6eo4kXvCyf2UmVGmKfJXvm1JTS4o6Lk1wIDK-RepcUSHPkZHFT6jCDw==';
export const mockCBHash = 'ZmVFcfCTuGi5YYpGTB_xzeYC6SfP_ernBOsakP-iq64=';
export const mockCoinbaseTransactionJSON: TransactionJSON = {
  version: 1,
  inputs: [
    {
      tx_out_hash: COINBASE_HASH,
      tx_out_index: 0,
      script: {
        type: SCRIPT_TYPE,
        pubkey: mockKeyPair.publicKey.valueOf(),
        sig: mockCBSig,
      },
    },
  ],
  outputs: [
    {
      value: mockTransactionValue,
      script: {
        type: SCRIPT_TYPE,
        pubkey_hash: Hash.fromPublicKey(mockKeyPair.publicKey).valueOf(),
      },
    },
  ],
  lock_time: 0,
};
export const mockTransactionHash = 'Sl_DPZl-qlXhjkudiZoSG9VscKU_cxm6AwBZzBEMK4M=';
export const mockTransactionState: TransactionState = {
  version: 1,
  inputs: [
    {
      txOutHash: mockCBHash,
      txOutIndex: 0,
      script: {
        type: SCRIPT_TYPE,
        publicKey: mockKeyPair.publicKey.valueOf(),
        signature:
          'cvIw1mKe52lJz5XRWIRTRWh-ztMcOJY6pYA9_GKmIQZB0c_1qu4hWlmD5VSft4gT1quSRptS5NOKbF6KDZjACw==',
      },
    },
  ],
  outputs: [
    {
      value: 100,
      script: {
        type: SCRIPT_TYPE,
        publicKeyHash: Hash.fromPublicKey(mockKeyPair.publicKey).valueOf(),
      },
    },
  ],
  transactionId: mockTransactionHash,
  lockTime: 0,
};
