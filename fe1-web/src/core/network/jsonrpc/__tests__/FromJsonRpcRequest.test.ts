import 'jest-extended';

import '__tests__/utils/matchers';
import keyPair from 'test_data/keypair.json';
import { OpenedLaoStore } from 'features/lao/store';
import { CreateLao } from 'features/lao/network/messages';
import { Lao } from 'features/lao/objects';
import { Base64UrlData, Hash, PrivateKey, PublicKey } from 'core/objects';
import { ROOT_CHANNEL } from 'core/objects/Channel';

import { JsonRpcMethod, JsonRpcRequest } from '../index';
import { MessageRegistry } from '../messages';
import { configureMessages } from '../messages/Message';
import { JsonRpcParamsWithMessage } from '../JsonRpcParamsWithMessage';

const JSON_RPC_FIELDS: string[] = ['method', 'params', 'id', 'jsonrpc'];
const QUERY_FIELD_COUNT = JSON_RPC_FIELDS.length;

const METHODS = Object.keys(JsonRpcMethod)
  .filter((key) => !key.includes('INVALID')) // remove INVALID method
  // @ts-ignore
  .map((key: string) => JsonRpcMethod[key]); // transform into array

export const mockPublicKey = new PublicKey(keyPair.publicKey);
export const mockSecretKey = new PrivateKey(keyPair.privateKey);

const messageRegistry = new MessageRegistry();
configureMessages(messageRegistry);

function checkRpcQuery(obj: any): void {
  expect(obj).toBeObject();
  expect(Object.keys(obj).length).toBe(QUERY_FIELD_COUNT);
  expect(obj).toContainAllKeys(JSON_RPC_FIELDS);

  expect(obj.method).toBeOneOf(METHODS);
  expect(obj.params).toBeObject();
  expect(obj.id).toBeNumber();
}

function checkRpcParams(obj: any, isRoot: boolean = false): void {
  expect(obj).toBeObject();
  expect(obj).toContainAllKeys(['channel', 'message']);
  expect(obj.channel).toBeString();
  if (isRoot) {
    expect(obj.channel).toBe(ROOT_CHANNEL);
  } else {
    expect(obj.channel).toMatch(/\/root\/[A-Za-z0-9+/]*[=]*/);
    expect(obj.channel.slice(ROOT_CHANNEL.length + 1)).toBeBase64Url();
  }
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
  const hashExpected = Hash.fromStringArray(obj.data, obj.signature);
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
  id: Hash.fromStringArray(mockPublicKey.toString(), '1613495222', 'Random Name').toString(),
});

const sampleCreateLaoDataString: string = JSON.stringify(sampleCreateLaoData);

function embeddedMessage(
  data: string,
  method: JsonRpcMethod = JsonRpcMethod.PUBLISH,
  channel: string = '/root/bGFvX2lk', // note: hardcoded value for testing
  id: number = 0,
): string {
  const data64: Base64UrlData = Base64UrlData.encode(data);
  return `{
        "jsonrpc": "2.0",
        "method": "${method.toString()}",
        "params": {
            "channel": "${channel}",
            "message": {
                "data": "${data64.toString()}",
                "sender": "${mockPublicKey.toString()}",
                "signature": "${mockSecretKey.sign(data64).toString()}",
                "message_id": "${Hash.fromStringArray(
                  data64.toString(),
                  mockSecretKey.sign(data64).toString(),
                )}",
                "witness_signatures": [
                ]
            }
        },
        "id": ${id}
      }
    `;
}

describe('=== fromJsonJsonRpcRequest checks ===', () => {
  beforeAll(() => {
    const sampleLao: Lao = new Lao({
      name: sampleCreateLaoData.name,
      id: Hash.fromStringArray(
        sampleCreateLaoData.organizer.toString(),
        sampleCreateLaoData.creation.toString(),
        sampleCreateLaoData.name,
      ),
      creation: sampleCreateLaoData.creation,
      last_modified: sampleCreateLaoData.creation,
      organizer: sampleCreateLaoData.organizer,
      witnesses: sampleCreateLaoData.witnesses,
    });

    OpenedLaoStore.store(sampleLao);
  });

  describe('should successfully create objects from Json', () => {
    const checkTypicalQuery = (query: any, isRoot: boolean = false) => {
      checkRpcQuery(query);

      checkRpcParams(query.params, isRoot);

      const msg = (query.params as JsonRpcParamsWithMessage).message;
      checkMessage(msg);

      const msgData = JSON.parse(msg.data.decode());
      expect(msgData).toBeJsonEqual(msg.messageData);
    };

    it('using a sub-channel', () => {
      const query = JsonRpcRequest.fromJson(embeddedMessage(sampleCreateLaoDataString));
      checkRpcQuery(query);
      checkRpcParams(query.params);

      const msg = (query.params as JsonRpcParamsWithMessage).message;
      checkMessage(msg);

      const msgData = JSON.parse(msg.data.decode());
      expect(msgData).toBeJsonEqual(msg.messageData);
    });

    it(`using '${ROOT_CHANNEL}' channel`, () => {
      const query = JsonRpcRequest.fromJson(
        embeddedMessage(sampleCreateLaoDataString, JsonRpcMethod.PUBLISH, ROOT_CHANNEL, 23),
      );
      checkTypicalQuery(query, true);
    });
  });
});
