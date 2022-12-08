import 'jest-extended';
import '__tests__/utils/matchers';

import keyPair from 'test_data/keypair.json';

import { configureTestFeatures, mockChannel } from '__tests__/utils';
import { Base64UrlData, Hash, PrivateKey, PublicKey, ROOT_CHANNEL } from 'core/objects';
import { CreateLao } from 'features/lao/network/messages';

import { JsonRpcMethod, JsonRpcRequest } from '../index';
import { JsonRpcParamsWithMessage } from '../JsonRpcParamsWithMessage';

const JSON_RPC_FIELDS: string[] = ['method', 'params', 'id', 'jsonrpc'];
const QUERY_FIELD_COUNT = JSON_RPC_FIELDS.length;

const METHODS = Object.keys(JsonRpcMethod)
  .filter((key) => !key.includes('INVALID')) // remove INVALID method
  // @ts-ignore
  .map((key: string) => JsonRpcMethod[key]); // transform into array

export const mockPublicKey = new PublicKey(keyPair.publicKey);
export const mockSecretKey = new PrivateKey(keyPair.privateKey);

function checkRpcQuery(obj: any): void {
  expect(obj).toBeObject();
  expect(Object.keys(obj).length).toBe(QUERY_FIELD_COUNT);
  expect(obj).toContainAllKeys(JSON_RPC_FIELDS);

  expect(obj.method).toBeOneOf(METHODS);
  expect(obj.params).toBeObject();
  expect(obj.id).toBeNumber();
}

function checkRpcParams(obj: any, channel: string): void {
  expect(obj).toBeObject();
  expect(obj).toContainAllKeys(['channel', 'message']);
  expect(obj.channel).toBeString();
  expect(obj.channel).toBe(channel);
  expect(obj.message).toBeObject();
}

function checkMessage(obj: any): void {
  expect(obj).toBeObject();
  expect(obj).toContainAllKeys(['data', 'sender', 'signature', 'message_id', 'witness_signatures']);

  expect(obj.data).toBeBase64Url();

  expect(obj.sender).toBeBase64Url();
  expect(obj.sender).toBeJsonEqual(mockPublicKey);

  expect(obj.signature).toBeBase64Url();
  const signExpected = mockSecretKey.sign(obj.data);
  expect(obj.signature).toBeJsonEqual(signExpected);

  expect(obj.message_id).toBeBase64Url();
  const hashExpected = Hash.fromArray(obj.data, obj.signature);
  expect(obj.message_id).toBeJsonEqual(hashExpected);

  expect(obj.witness_signatures).toBeKeySignatureArray('publicKey', 'signature');
}

const sampleCreateLaoData: CreateLao = CreateLao.fromJson({
  object: 'lao',
  action: 'create',
  name: 'Random Name',
  creation: 1613495222,
  organizer: mockPublicKey.toString(),
  witnesses: [],
  id: Hash.fromArray(mockPublicKey, '1613495222', 'Random Name').toString(),
});

const sampleCreateLaoDataString: string = JSON.stringify(sampleCreateLaoData);

function embeddedMessage(data: string, channel: string, id: number = 0): string {
  const data64: Base64UrlData = Base64UrlData.encode(data);
  return `{
        "jsonrpc": "2.0",
        "method": "${JsonRpcMethod.PUBLISH.toString()}",
        "params": {
            "channel": "${channel}",
            "message": {
                "data": "${data64.toString()}",
                "sender": "${mockPublicKey.toString()}",
                "signature": "${mockSecretKey.sign(data64).toString()}",
                "message_id": "${Hash.fromArray(data64, mockSecretKey.sign(data64))}",
                "witness_signatures": [
                ]
            }
        },
        "id": ${id}
      }
    `;
}

describe('FromJsonRpcRequest should successfully create objects from Json', () => {
  beforeAll(() => {
    configureTestFeatures();
  });

  const verify = (jsonString: string, channel: string) => {
    const query = JsonRpcRequest.fromJson(jsonString);
    checkRpcQuery(query);
    checkRpcParams(query.params, channel);

    const msg = (query.params as JsonRpcParamsWithMessage).message;
    checkMessage(msg);

    const msgData = JSON.parse(msg.data.decode());
    expect(msgData).toBeJsonEqual(msg.messageData);
  };

  it('using a sub-channel', () => {
    const msg = embeddedMessage(sampleCreateLaoDataString, mockChannel);
    verify(msg, mockChannel);
  });

  it(`using '${ROOT_CHANNEL}' channel`, () => {
    const msg = embeddedMessage(sampleCreateLaoDataString, ROOT_CHANNEL, 23);
    verify(msg, ROOT_CHANNEL);
  });
});
