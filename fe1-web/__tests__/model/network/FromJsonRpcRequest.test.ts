import 'jest-extended';
import '../../utils/matchers';

import { CreateLao } from 'model/network/method/message/data';
import { storeInit } from 'store/Storage';
import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import {
  Base64Data, Hash, Lao, PrivateKey, PublicKey,
} from 'model/objects';
import { OpenedLaoStore } from 'store';
import { JsonRpcParamsWithMessage } from 'model/network/method/JsonRpcParamsWithMessage';
import { ROOT_CHANNEL } from 'model/objects/Channel';

const JSON_RPC_FIELDS: string[] = ['method', 'params', 'id']; // jsonrpc version is not stored, thus only 3 fields
const QUERY_FIELD_COUNT = JSON_RPC_FIELDS.length;

const METHODS = Object.keys(JsonRpcMethod)
  .filter((key) => !key.includes('INVALID')) // remove INVALID method
  // @ts-ignore
  .map((key: string) => JsonRpcMethod[key]); // transform into array

export const mockPublicKey = new PublicKey('xjHAz+d0udy1XfHp5qugskWJVEGZETN/8DV3+ccOFSs=');
export const mockSecretKey = new PrivateKey('vx0b2hbxwPBQzfPu9NdlCcYmuFjhUFuIUDx6doHRCM7GMcDP53S53LVd8enmq6CyRYlUQZkRM3/wNXf5xw4VKw==');

function checkQueryOuterLayer(obj: any): void {
  expect(obj).toBeObject();
  expect(Object.keys(obj).length).toBe(QUERY_FIELD_COUNT);
  expect(obj).toContainAllKeys(JSON_RPC_FIELDS);

  expect(obj.method).toBeOneOf(METHODS);
  expect(obj.params).toBeObject();
  expect(obj.id).toBeNumber();
}

function checkParams(obj: any, isRoot: boolean = false): void {
  expect(obj).toBeObject();
  expect(obj).toContainAllKeys(['channel', 'message']);
  expect(obj.channel).toBeString();
  if (isRoot) {
    expect(obj.channel).toBe(ROOT_CHANNEL);
  } else {
    expect(obj.channel).toMatch(/\/root\/[A-Za-z0-9+/]*[=]*/);
    expect(obj.channel.slice(ROOT_CHANNEL.length + 1)).toBeBase64();
  }
  expect(obj.message).toBeObject();
}

function checkMessage(obj: any): void {
  expect(obj).toBeObject();
  expect(obj).toContainAllKeys(['data', 'sender', 'signature', 'message_id', 'witness_signatures', 'messageData']);

  expect(obj.data).toBeBase64();

  expect(obj.sender).toBeBase64();
  expect(obj.sender).toBeJsonEqual(mockPublicKey);

  expect(obj.signature).toBeBase64();
  const signExpected = new PrivateKey(mockSecretKey.valueOf()).sign(obj.data);
  expect(obj.signature).toBeJsonEqual(signExpected);

  expect(obj.message_id).toBeBase64();
  const hashExpected = Hash.fromStringArray(obj.data, obj.signature);
  expect(obj.message_id).toBeJsonEqual(hashExpected);

  expect(Array.isArray(obj.witness_signatures)).toBe(true);
  obj.witness_signatures.forEach((witSig: any) => {
    expect(witSig.publicKey).toBeBase64();
    expect(witSig.signature).toBeBase64();
  });
}

function compareQueryMessageData(query: JsonRpcRequest): void {
  const data64 = (((query.params as JsonRpcParamsWithMessage).message.data) as Base64Data);

  expect(JSON.parse(data64.decode())).toEqual(
    (query.params as JsonRpcParamsWithMessage).message.messageData,
  );
}

describe('=== fromJsonJsonRpcRequest checks ===', () => {
  const sampleCreateLaoData: CreateLao = CreateLao.fromJson({
    object: 'lao',
    action: 'create',
    name: 'Random Name',
    creation: 1613495222,
    organizer: mockPublicKey.toString(),
    witnesses: [],
    id: Hash.fromStringArray(mockPublicKey.toString(), '1613495222', 'Random Name').toString(),
  });

  beforeAll(() => {
    storeInit();

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

  const sampleCreateLaoDataString: string = JSON.stringify(sampleCreateLaoData);

  function embeddedMessage(
    data: string,
    method: JsonRpcMethod = JsonRpcMethod.PUBLISH,
    channel: string = '/root/bGFvX2lk', // note: hardcoded value for testing
    id: number = 0,
  ): string {
    const data64: Base64Data = Base64Data.encode(data);
    return `{
        "jsonrpc": "2.0",
        "method": "${method.toString()}",
        "params": {
            "channel": "${channel}",
            "message": {
                "data": "${data64.toString()}",
                "sender": "${mockPublicKey.toString()}",
                "signature": "${mockSecretKey.sign(data64).toString()}",
                "message_id": "${Hash.fromStringArray(data64.toString(), mockSecretKey.sign(data64).toString())}",
                "witness_signatures": [
                ]
            }
        },
        "id": ${id}
      }
    `;
  }

  describe('should successfully create objects from Json', () => {
    const checkTypicalQuery = (query: any, isRoot: boolean = false) => {
      checkQueryOuterLayer(query);
      checkParams(query.params, isRoot);
      checkMessage((query.params as JsonRpcParamsWithMessage).message);
      compareQueryMessageData(query);
    };

    it('using a sub-channel', () => {
      const query = JsonRpcRequest.fromJson(embeddedMessage(sampleCreateLaoDataString));
      checkTypicalQuery(query);
    });

    it(`using '${ROOT_CHANNEL}' channel`, () => {
      const query = JsonRpcRequest.fromJson(
        embeddedMessage(sampleCreateLaoDataString, JsonRpcMethod.PUBLISH, ROOT_CHANNEL, 23),
      );
      checkTypicalQuery(query, true);
    });
  });
});
