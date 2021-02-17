/* eslint-disable */

import { CreateLao } from 'model/network/method/message/data';
import { storeInit } from 'store/Storage';
import { JsonRpcMethod, JsonRpcRequest } from 'model/network';
import { Base64Data, Hash, Lao, PrivateKey, PublicKey } from 'model/objects';
import { OpenedLaoStore } from 'store';
import * as b64 from 'base-64';
import { JsonRpcParamsWithMessage } from 'model/network/method/JsonRpcParamsWithMessage';
import { ROOT_CHANNEL } from 'model/objects/Channel';

const assertChai = require('chai').assert;



const JSON_RPC_FIELDS: string[] = ['method', 'params', 'id']; // jsonrpc version is not stored, thus only 3 fields
const QUERY_FIELD_COUNT = JSON_RPC_FIELDS.length;

const METHODS = Object.keys(JsonRpcMethod)
  .filter(key => !key.includes('INVALID')) // remove INVALID method
  // @ts-ignore
  .map((key: string) => JsonRpcMethod[key]); // transform into array

export const mockPublicKey = new PublicKey('xjHAz+d0udy1XfHp5qugskWJVEGZETN/8DV3+ccOFSs=');
export const mockSecretKey = new PrivateKey('vx0b2hbxwPBQzfPu9NdlCcYmuFjhUFuIUDx6doHRCM7GMcDP53S53LVd8enmq6CyRYlUQZkRM3/wNXf5xw4VKw==');


function checkIsBase64String(str: Base64Data): boolean {
  try { b64.decode(str.toString()); } catch (error) { return false; }
  return true;
}

function checkQueryOuterLayer(obj: any): void {
  assertChai.isObject(obj, `the query should be a JSON object but is ${typeof obj}`);
  assertChai.strictEqual(
    Object.keys(obj).length,
    QUERY_FIELD_COUNT,
    `the query should have ${QUERY_FIELD_COUNT} fields but has ${Object.keys(obj).length}`
  );
  assertChai.hasAllKeys(obj, JSON_RPC_FIELDS);

  assertChai.oneOf(obj.method, METHODS, 'unknown method: ' + obj.method.toString());
  assertChai.isObject(obj.params);
  assertChai.isNumber(obj.id);
}

function checkParams(obj: any, isRoot: boolean = false): void {

  assertChai.isObject(obj, `the params should be a JSON object but is ${typeof obj}`);
  assertChai.hasAllKeys(obj, ['channel', 'message']);
  assertChai.isString(obj.channel);
  assertChai.isString(obj.channel, `the channel should be a string but is a ${typeof obj.channel}`);
  if (isRoot) assertChai.strictEqual(obj.channel, ROOT_CHANNEL, `the channel should be "${ROOT_CHANNEL}" but is "${obj.channel}"`);
  else {
    assertChai.match(obj.channel, /\/root\/[A-Za-z0-9+\/]*[=]*/, 'the channel should start with "/root/" and be followed by a base64 string');
    assertChai.isTrue(
      checkIsBase64String(obj.channel.slice(ROOT_CHANNEL.length + 1)),
      'the channel\' lao id (after "/root/") is not base64 encoded. Actual: ' + obj.channel
    );
  }
  assertChai.isObject(obj.message);
}

function checkMessage(obj: any): void {
  assertChai.isObject(obj, `the message should be a JSON object but is ${typeof obj}`);
  assertChai.hasAllKeys(obj, ['data', 'sender', 'signature', 'message_id', 'witness_signatures', 'messageData']);

  assertChai.isString(obj.data);
  assertChai.isTrue(
    checkIsBase64String(obj.data),
    'the query data should be base64 encoded, but is not a valid base64 string'
  );

  assertChai.isString(obj.sender);
  assertChai.isTrue(
    checkIsBase64String(obj.sender),
    'the query data should be base64 encoded, but is not a valid base64 string'
  );
  assertChai.strictEqual(obj.sender.toString(), mockPublicKey.toString(), `the sender public key is not self. Actual: "${obj.sender}", Expected: "${mockPublicKey}"`);

  assertChai.isString(obj.signature);
  assertChai.isTrue(
    checkIsBase64String(obj.signature),
    'the query data should be base64 encoded, but is not a valid base64 string'
  );/*
  const signExpected = wsUtils.signString(b64.decode(obj.data), mockSecretKey);
  assertChai.strictEqual(
    obj.signature,
    signExpected,
    `the signature does not correspond to the expected one. Actual "${obj.signature}", Expected: "${signExpected}"`
  );

  assertChai.isString(obj.message_id);
  assertChai.isTrue(
    checkIsBase64String(obj.message_id),
    'the query data should be base64 encoded, but is not a valid base64 string'
  );
  const hashExpected = wsUtils.hashStrings(obj.data, obj.signature);
  assertChai.strictEqual(
    obj.message_id,
    hashExpected,
    `the message_id does not correspond to the expected one. Actual "${obj.message_id}", Expected: "${hashExpected}"`
  );

  assertChai.isArray(obj.witness_signatures);
  assertChai.isTrue(checkArrayKeySignPairIsBase64(obj.witness_signatures), 'the witness_signatures should only contain public keys');*/
}

function compareQueryMessageData(query: JsonRpcRequest): void {
  const data64 = (((query.params as JsonRpcParamsWithMessage).message.data) as Base64Data);

  assertChai.deepEqual(
    JSON.parse(data64.decode()),
    ((query.params as JsonRpcParamsWithMessage).message.messageData)
  );
}



describe('=== fromJsonJsonRpcRequest checks ===', function() {

  beforeAll(() => {
    storeInit();

    const sampleLao: Lao = new Lao({
      name: sampleCreateLaoData.name,
      id: Hash.fromStringArray(sampleCreateLaoData.organizer.toString(), sampleCreateLaoData.creation.toString(), sampleCreateLaoData.name),
      creation: sampleCreateLaoData.creation,
      last_modified: sampleCreateLaoData.creation,
      organizer: sampleCreateLaoData.organizer,
      witnesses: sampleCreateLaoData.witnesses,
    });

    OpenedLaoStore.store(sampleLao);
  });



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
    channel: string = "/root/bGFvX2lk", // note: hardcoded value for testing
    id: number = 0
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



  describe('should successfully create objects from Json', function () {

    const checkTypicalQuery = (query: any, isRoot: boolean = false) => {
      checkQueryOuterLayer(query);
      checkParams(query.params, isRoot);
      checkMessage((query.params as JsonRpcParamsWithMessage).message);
      compareQueryMessageData(query);
    };

    it('using a sub-channel', function () {
      const query = JsonRpcRequest.fromJson(embeddedMessage(sampleCreateLaoDataString));
      checkTypicalQuery(query);
    });

    it('using \'' + ROOT_CHANNEL + '\' channel', function () {
      const query = JsonRpcRequest.fromJson(embeddedMessage(sampleCreateLaoDataString, JsonRpcMethod.PUBLISH, ROOT_CHANNEL, 23));
      checkTypicalQuery(query, true);
    });
  });
});
