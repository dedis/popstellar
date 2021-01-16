/* eslint-disable */

import * as wsUtils from '../../websockets/WebsocketUtils';
import {
  mockCreationTime,
  mockCurrentLao,
  mockEventName,
  mockPublicKey,
  mockSecretKey,
  mockLocation
} from './WebsocketApi';
const assertChai = require('chai').assert;

const QUERY_FIELD_COUNT = 4;
const ROOT_CHANNEL = '/root';
const METHODS = ['publish', 'subscribe', 'message', 'unsubscribe', 'catchup'];
const OBJECTS = ['lao'];
const ACTIONS = ['create', 'update_properties', 'state'];


/* ----------------------------------------- CHECK UTILS ---------------------------------------- */
const checkIsBase64String = (str) => {
  if (typeof str !== 'string') return false;
  try { atob(str); } catch (error) { return false; }
  return true;
};

const checkArrayIsBase64 = (arr) => {
  const pubKeyLen = mockPublicKey.length;
  if (!Array.isArray(arr)) return false;

  const res = arr.filter(item => (!checkIsBase64String(item) || item.length !== pubKeyLen));
  return (res.length === 0);
};

const checkArrayKeySignPairIsBase64 = (arr) => {
  const pubKeyLen = mockPublicKey.length;
  if (!Array.isArray(arr)) return false;

  arr.forEach((item) => {
    if (typeof item !== 'object' || Object.keys(item).length !== 2) return false;
    if (!(
      Object.prototype.hasOwnProperty.call(item, 'witness')
      && Object.prototype.hasOwnProperty.call(item, 'signature')
    )) return false;

    if (!checkIsBase64String(item.witness) || !checkIsBase64String(item.signature)) return false;
    if (item.witness.length !== pubKeyLen) return false;
    // not checked : signature correctness
  });
  return true;
};


/* ------------------------------------- CHECK COMMON PARTS ------------------------------------- */
export const checkQueryOuterLayer = (obj) => {
  assertChai.isObject(obj, `the query should be a JSON object but is ${typeof obj}`);
  assertChai.strictEqual(
    Object.keys(obj).length,
    QUERY_FIELD_COUNT,
    `the query should have ${QUERY_FIELD_COUNT} fields but has ${Object.keys(obj).length}`
  );
  assertChai.hasAllKeys(obj, ['jsonrpc', 'method', 'params', 'id']);
  assertChai.strictEqual(
    obj.jsonrpc,
    wsUtils.JSON_RPC_VERSION,
    'JSON-RPC version should be the string 2.0'
  );
  assertChai.oneOf(obj.method, METHODS, 'unknown method: ' + obj.method.toString());
  assertChai.isObject(obj.params);
  assertChai.isNumber(obj.id);
};


export const checkParams = (obj, isRoot = false) => {
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
};


export const checkMessage = (obj) => {
  assertChai.isObject(obj, `the message should be a JSON object but is ${typeof obj}`);
  assertChai.hasAllKeys(obj, ['data', 'sender', 'signature', 'message_id', 'witness_signatures']);

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
  assertChai.strictEqual(obj.sender, mockPublicKey, `the sender public key is not self. Actual: "${obj.sender}", Expected: "${mockPublicKey}"`);

  assertChai.isString(obj.signature);
  assertChai.isTrue(
    checkIsBase64String(obj.signature),
    'the query data should be base64 encoded, but is not a valid base64 string'
  );
  const signExpected = wsUtils.signString(atob(obj.data), mockSecretKey);
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
  assertChai.isTrue(checkArrayKeySignPairIsBase64(obj.witness_signatures), 'the witness_signatures should only contain public keys');
};


/* --------------------------------- CHECK RANDOM LITTLE THINGS --------------------------------- */
export const checkQueryMethod = (request, method) => {
  assertChai.strictEqual(request.method, method, `the query's method ${request.method} should be ${method}`);
};


export const checkQueryEmptyWitnessSignature = (obj) => {
  const _ws = obj.params.message.witness_signatures;
  assertChai.isEmpty(_ws, `the witness_signatures array should be an empty array but has ${_ws.length} element(s)`);
};


/* -------------------------------------- CHECK DATA FIELDS ------------------------------------- */

const _descriptionStart = 'the query\'s decoded data';
const _defaultDataFields = ['object', 'action'];

const _arrayToString = (arr) => {
  let res = '';
  arr.forEach(e => res += (e + ','));
  return `[${res.slice(0, -1)}]`;
};

const _checkDataHeaders = (actualObject, expectedObject, storedObject, actualAction, expectedAction, storedAction) => {
  assertChai.isString(actualObject);
  assertChai.strictEqual(actualObject, expectedObject, `${_descriptionStart} object field should be "${expectedObject}"`);
  assertChai.strictEqual(storedObject, expectedObject, `the stored object (in the pendingQueries map) should be "${expectedObject}"`);

  assertChai.isString(expectedAction);
  assertChai.strictEqual(expectedAction, expectedAction, `${_descriptionStart} action field should be "${expectedAction}"`);
  assertChai.strictEqual(storedAction, expectedAction, `the stored action (in the pendingQueries map) should be "${expectedAction}"`);
};

export const checkQueryDataCreateLao = (request, object, action) => {

  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedFields = _defaultDataFields.concat(['id', 'name', 'creation', 'organizer', 'witnesses']);
  assertChai.hasAllKeys(
    data,
    expectedFields,
    `${_descriptionStart} fields contains unknown/is missing some fields. Expected : ${_arrayToString(expectedFields)}`
  );

  _checkDataHeaders(data.object, 'lao', object, data.action, 'create', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isString(data.name, `${_descriptionStart} name field should be a string. Actual : ${typeof data.name}`);
  assertChai.strictEqual(data.name, mockEventName);

  assertChai.isNumber(data.creation);
  assertChai.isAbove(data.creation, 0, `${_descriptionStart} creation field should be greater than 0 but is ${data.creation}`);

  assertChai.isString(data.organizer);
  assertChai.isTrue(checkIsBase64String(data.organizer), `${_descriptionStart} organizer field should be base64 encoded. Actual : ${data.organizer}`);
  assertChai.strictEqual(data.organizer, mockPublicKey, `${_descriptionStart} organizer field should correspond to the public key that created the request`);

  assertChai.isArray(data.witnesses);
  assertChai.isTrue(checkArrayIsBase64(data.witnesses), `${_descriptionStart} witnesses field should be an array of base64 strings with correct length (key length = ${mockPublicKey.length})`);
  assertChai.strictEqual(data.witnesses.length, [...new Set(data.witnesses)].length, `${_descriptionStart} witnesses field array should only contain distinct values`);

  // check id
  expected = wsUtils.hashStrings(data.organizer, data.creation, data.name);
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataUpdateLao = (request, object, action) => {

  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedFields = _defaultDataFields.concat(['id', 'name', 'last_modified', 'witnesses']);
  assertChai.hasAllKeys(
    data,
    expectedFields,
    `${_descriptionStart} fields contains unknown/is missing some fields. Expected : ${_arrayToString(expectedFields)}`
  );

  _checkDataHeaders(data.object, 'lao', object, data.action, 'update_properties', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isString(data.name, `${_descriptionStart} name field should be a string. Actual : ${typeof data.name}`);
  assertChai.strictEqual(data.name, mockEventName);

  assertChai.isNumber(data.last_modified);
  assertChai.isAbove(data.last_modified, 0, `${_descriptionStart} last_modified field should be greater than 0 but is ${data.last_modified}`);

  assertChai.isArray(data.witnesses);
  assertChai.isTrue(checkArrayIsBase64(data.witnesses), `${_descriptionStart} witnesses field should be an array of base64 strings with correct length (key length = ${mockPublicKey.length})`);
  assertChai.strictEqual(data.witnesses.length, [...new Set(data.witnesses)].length, `${_descriptionStart} witnesses field array should only contain distinct values`);

  // check id
  expected = wsUtils.hashStrings(mockCurrentLao.params.message.data.organizer, mockCurrentLao.params.message.data.creation, data.name);
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataStateLao = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedFields = _defaultDataFields.concat(['id', 'name', 'creation', 'last_modified', 'organizer', 'witnesses', 'modification_id', 'modification_signatures']);
  assertChai.hasAllKeys(
    data,
    expectedFields,
    `${_descriptionStart} fields contains unknown/is missing some fields. Expected : ${_arrayToString(expectedFields)}`
  );

  _checkDataHeaders(data.object, 'lao', object, data.action, 'state', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isString(data.name, `${_descriptionStart} name field should be a string. Actual : ${typeof data.name}`);
  assertChai.strictEqual(data.name, mockCurrentLao.params.message.data.name);

  assertChai.isNumber(data.creation);
  assertChai.isAbove(data.creation, 0, `${_descriptionStart} creation field should be greater than 0 but is ${data.creation}`);

  assertChai.isNumber(data.last_modified);
  assertChai.isAbove(data.last_modified, 0, `${_descriptionStart} last_modified field should be greater than 0 but is ${data.last_modified}`);
  assertChai.isAbove(data.last_modified + 1, data.creation, `${_descriptionStart} last_modified field should be greater than creation (${data.creation}) but is ${data.last_modified}`);

  assertChai.isString(data.organizer);
  assertChai.isTrue(checkIsBase64String(data.organizer), `${_descriptionStart} organizer field should be base64 encoded. Actual : ${data.organizer}`);
  assertChai.strictEqual(data.organizer, mockPublicKey, `${_descriptionStart} organizer field should correspond to the public key that created the request`);

  assertChai.isArray(data.witnesses);
  assertChai.isTrue(checkArrayIsBase64(data.witnesses), `${_descriptionStart} witnesses field should be an array of base64 strings with correct length (key length = ${mockPublicKey.length})`);
  assertChai.strictEqual(data.witnesses.length, [...new Set(data.witnesses)].length, `${_descriptionStart} witnesses field array should only contain distinct values`);

  assertChai.isString(data.modification_id);
  assertChai.isTrue(checkIsBase64String(data.modification_id), `${_descriptionStart} modification_id field should be base64 encoded. Actual : ${data.modification_id}`);

  assertChai.isArray(data.modification_signatures);
  assertChai.isTrue(checkArrayKeySignPairIsBase64(data.modification_signatures), `${_descriptionStart} modification_signatures field should contain base64 encoded key sign pairs. Actual : ${data.modification_signatures}`);

  // check id
  expected = wsUtils.hashStrings(data.organizer, data.creation, data.name);
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataMeeting = (request, object, action) => {
  if (action === 'create') checkQueryDataCreateMeeting(request, object, action);
  else if (action === 'state') checkQueryDataStateMeeting(request, object, action);
  else assertChai.fail(`the data action field for a ${object} is unknown. Actual : ${action}`);
};


export const checkQueryDataCreateMeeting = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedMinFields = _defaultDataFields.concat(['id', 'name', 'creation', 'start']);
  const optionalFields = ['location', 'end', 'extra'];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every(field => possibleFields.includes(field));
  assertChai.isTrue(isSubsetOfPossibleFields, `${_descriptionStart} contains additional unknown fields. Possible fields : ${_arrayToString(possibleFields)}, Actual fields : ${_arrayToString(Object.getOwnPropertyNames(data))}`);

  _checkDataHeaders(data.object, 'meeting', object, data.action, 'create', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isString(data.name, `${_descriptionStart} name field should be a string. Actual : ${typeof data.name}`);
  assertChai.strictEqual(data.name, mockEventName);

  assertChai.isNumber(data.creation);
  assertChai.isAbove(data.creation, 0, `${_descriptionStart} creation field should be greater than 0 but is ${data.creation}`);

  if (Object.prototype.hasOwnProperty.call(data, 'location')) {
    assertChai.isString(data.location, `${_descriptionStart} location field should be a string. Actual : ${typeof data.location}`);
    assertChai.strictEqual(data.location, mockLocation);
  }

  assertChai.isNumber(data.start);
  assertChai.isAbove(data.start, 0, `${_descriptionStart} start field should be greater than 0 but is ${data.start}`);
  assertChai.isAbove(data.start + 1, data.creation, `${_descriptionStart} start field should be greater than or equal to creation (${data.creation}) but is ${data.start}`);

  if (Object.prototype.hasOwnProperty.call(data, 'end')) {
    assertChai.isNumber(data.end);
    assertChai.isAbove(data.end, 0, `${_descriptionStart} end field should be greater than 0 but is ${data.end}`);
    assertChai.isAbove(data.end + 1, data.start, `${_descriptionStart} end field should be greater than or equal to start (${data.start}) but is ${data.end}`);
  }

  if (Object.prototype.hasOwnProperty.call(data, 'extra')) {
    assertChai.isObject(data.extra);
  }

  // check id
  expected = wsUtils.hashStrings('M', mockCurrentLao.params.message.data.id, data.creation, data.name);
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataStateMeeting = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedMinFields = _defaultDataFields.concat(["id", "name", "creation", "last_modified", "start", "modification_id", "modification_signatures"]);
  const optionalFields = ['location', 'end', 'extra'];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every(field => possibleFields.includes(field));
  assertChai.isTrue(isSubsetOfPossibleFields, `${_descriptionStart} contains additional unknown fields. Possible fields : ${_arrayToString(possibleFields)}, Actual fields : ${_arrayToString(Object.getOwnPropertyNames(data))}`);

  _checkDataHeaders(data.object, 'meeting', object, data.action, 'state', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isString(data.name, `${_descriptionStart} name field should be a string. Actual : ${typeof data.name}`);
  assertChai.strictEqual(data.name, mockCurrentLao.params.message.data.name);

  assertChai.isNumber(data.creation);
  assertChai.isAbove(data.creation, 0, `${_descriptionStart} creation field should be greater than 0 but is ${data.creation}`);

  assertChai.isNumber(data.last_modified);
  assertChai.isAbove(data.last_modified, 0, `${_descriptionStart} last_modified field should be greater than 0 but is ${data.last_modified}`);
  assertChai.isAbove(data.last_modified + 1, data.creation, `${_descriptionStart} last_modified field should be greater than or equal to creation (${data.creation}) but is ${data.last_modified}`);

  if (Object.prototype.hasOwnProperty.call(data, 'location')) {
    assertChai.isString(data.location, `${_descriptionStart} location field should be a string. Actual : ${typeof data.location}`);
    assertChai.strictEqual(data.location, mockLocation);
  }

  assertChai.isNumber(data.start);
  assertChai.isAbove(data.start, 0, `${_descriptionStart} start field should be greater than 0 but is ${data.start}`);
  assertChai.isAbove(data.start + 1, data.creation, `${_descriptionStart} start field should be greater than or equal to creation (${data.creation}) but is ${data.start}`);

  if (Object.prototype.hasOwnProperty.call(data, 'end')) {
    assertChai.isNumber(data.end);
    assertChai.isAbove(data.end, 0, `${_descriptionStart} end field should be greater than 0 but is ${data.end}`);
    assertChai.isAbove(data.end + 1, data.start, `${_descriptionStart} end field should be greater than or equal to start (${data.start}) but is ${data.end}`);
  }

  if (Object.prototype.hasOwnProperty.call(data, 'extra')) {
    assertChai.isObject(data.extra);
  }

  assertChai.isString(data.modification_id);
  assertChai.isTrue(checkIsBase64String(data.modification_id), `${_descriptionStart} modification_id field should be base64 encoded. Actual : ${data.modification_id}`);

  assertChai.isArray(data.modification_signatures);
  assertChai.isTrue(checkArrayKeySignPairIsBase64(data.modification_signatures), `${_descriptionStart} modification_signatures field should contain base64 encoded key sign pairs. Actual : ${data.modification_signatures}`);

  // check id
  expected = wsUtils.hashStrings('M', mockCurrentLao.params.message.data.id, data.creation, data.name);
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataRollCall = (request, object, action) => {
  if (action === 'create') checkQueryDataCreateRollCall(request, object, action);
  else if (action === 'open') checkQueryDataOpenRollCall(request, object, action);
  else if (action === 'reopen') checkQueryDataReopenRollCall(request, object, action);
  else if (action === 'close') checkQueryDataCloseRollCall(request, object, action);
  else assertChai.fail(`the data action field for a ${object} is unknown. Actual : ${action}`);
};


export const checkQueryDataCreateRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedMinFields = _defaultDataFields.concat(['id', 'name', 'creation', 'location']);
  const optionalFields = ['start', 'scheduled', 'roll_call_description'];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`
  );
  const xor = (Object.prototype.hasOwnProperty.call(data, 'start') && Object.prototype.hasOwnProperty.call(data, 'scheduled'));
  assertChai.isFalse(xor, 'the create roll call data field contains both a "start" and "scheduled" field');
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every(field => possibleFields.includes(field));
  assertChai.isTrue(isSubsetOfPossibleFields, `${_descriptionStart} contains additional unknown fields. Possible fields : ${_arrayToString(possibleFields)}, Actual fields : ${_arrayToString(Object.getOwnPropertyNames(data))}`);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'create', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isString(data.name, `${_descriptionStart} name field should be a string. Actual : ${typeof data.name}`);
  assertChai.strictEqual(data.name, mockEventName);

  assertChai.isNumber(data.creation);
  assertChai.isAbove(data.creation, 0, `${_descriptionStart} creation field should be greater than 0 but is ${data.creation}`);

  if (Object.prototype.hasOwnProperty.call(data, 'start')) {
    assertChai.isNumber(data.start);
    assertChai.isAbove(data.start, 0, `${_descriptionStart} start field should be greater than 0 but is ${data.start}`);
    assertChai.isAbove(data.start + 1, data.creation, `${_descriptionStart} start field should be greater than or equal to creation (${data.creation}) but is ${data.start}`);
  }

  if (Object.prototype.hasOwnProperty.call(data, 'scheduled')) {
    assertChai.isNumber(data.scheduled);
    assertChai.isAbove(data.scheduled, 0, `${_descriptionStart} scheduled field should be greater than 0 but is ${data.scheduled}`);
    assertChai.isAbove(data.scheduled + 1, data.creation, `${_descriptionStart} scheduled field should be greater than or equal to creation (${data.creation}) but is ${data.scheduled}`);
  }

  assertChai.isString(data.location, `${_descriptionStart} location field should be a string. Actual : ${typeof data.location}`);
  assertChai.strictEqual(data.location, mockLocation);

  if (Object.prototype.hasOwnProperty.call(data, 'roll_call_description')) {
    assertChai.isString(data.roll_call_description);
  }

  // check id
  expected = wsUtils.hashStrings('R', mockCurrentLao.params.message.data.id, data.creation, data.name);
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataOpenRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedMinFields = _defaultDataFields.concat(['id', 'start']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every(field => possibleFields.includes(field));
  assertChai.isTrue(isSubsetOfPossibleFields, `${_descriptionStart} contains additional unknown fields. Possible fields : ${_arrayToString(possibleFields)}, Actual fields : ${_arrayToString(Object.getOwnPropertyNames(data))}`);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'open', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isNumber(data.start);
  assertChai.isAbove(data.start, 0, `${_descriptionStart} start field should be greater than 0 but is ${data.start}`);

  // check id
  expected = wsUtils.hashStrings('R', mockCurrentLao.params.message.data.id, 444, 'r-cName'); // 444 and r-cName are for now hardocded in the APi
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataReopenRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedMinFields = _defaultDataFields.concat(['id', 'start']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every(field => possibleFields.includes(field));
  assertChai.isTrue(isSubsetOfPossibleFields, `${_descriptionStart} contains additional unknown fields. Possible fields : ${_arrayToString(possibleFields)}, Actual fields : ${_arrayToString(Object.getOwnPropertyNames(data))}`);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'reopen', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isNumber(data.start);
  assertChai.isAbove(data.start, 0, `${_descriptionStart} start field should be greater than 0 but is ${data.start}`);

  // check id
  expected = wsUtils.hashStrings('R', mockCurrentLao.params.message.data.id, 444, 'r-cName'); // 444 and r-cName are for now hardocded in the APi
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataCloseRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedMinFields = _defaultDataFields.concat(['id', 'start', 'end', 'attendees']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every(field => possibleFields.includes(field));
  assertChai.isTrue(isSubsetOfPossibleFields, `${_descriptionStart} contains additional unknown fields. Possible fields : ${_arrayToString(possibleFields)}, Actual fields : ${_arrayToString(Object.getOwnPropertyNames(data))}`);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'close', action);

  assertChai.isString(data.id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.id}`);
  assertChai.isTrue(checkIsBase64String(data.id), `${_descriptionStart} id field should be base64 encoded. Actual : ${data.id}`);

  assertChai.isNumber(data.start);
  assertChai.isAbove(data.start, 0, `${_descriptionStart} start field should be greater than 0 but is ${data.start}`);
  assertChai.strictEqual(data.start, 555); // 555 is for now hardcoded in the API

  assertChai.isNumber(data.end);
  assertChai.isAbove(data.end, 0, `${_descriptionStart} end field should be greater than 0 but is ${data.end}`);
  assertChai.isAbove(data.end + 1, data.start, `${_descriptionStart} end field should be greater than or equal to start (${data.start}) but is ${data.end}`);

  assertChai.isArray(data.attendees);
  assertChai.isTrue(checkArrayIsBase64(data.attendees), `${_descriptionStart} attendees field should be an array of base64 strings with correct length (key length = ${mockPublicKey.length})`);
  assertChai.strictEqual(data.attendees.length, [...new Set(data.attendees)].length, `${_descriptionStart} attendees field array should only contain distinct values`);

  // check id
  expected = wsUtils.hashStrings('R', mockCurrentLao.params.message.data.id, 444, 'r-cName'); // 444 and r-cName are for now hardocded in the APi
  assertChai.strictEqual(data.id, expected, `${_descriptionStart} id field "${data.id}" should be "${expected}"`);
};


export const checkQueryDataWitnessMessage = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(atob(dataEncoded));
  } catch (error) { assertChai.fail(`${_descriptionStart} should be JSON`); }

  assertChai.isObject(data, `${_descriptionStart} should be a JSON object but is a ${typeof data}`);
  const expectedMinFields = _defaultDataFields.concat(['message_id', 'signature']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every(field => possibleFields.includes(field));
  assertChai.isTrue(isSubsetOfPossibleFields, `${_descriptionStart} contains additional unknown fields. Possible fields : ${_arrayToString(possibleFields)}, Actual fields : ${_arrayToString(Object.getOwnPropertyNames(data))}`);

  _checkDataHeaders(data.object, 'message', object, data.action, 'witness', action);

  assertChai.isString(data.message_id, `${_descriptionStart} id field should be a string. Actual : ${typeof data.message_id}`);
  assertChai.isTrue(checkIsBase64String(data.message_id), `${_descriptionStart} message_id field should be base64 encoded. Actual : ${data.message_id}`);

  assertChai.isString(data.signature, `${_descriptionStart} signature field should be a string. Actual : ${typeof data.signature}`);
  assertChai.isTrue(checkIsBase64String(data.signature), `${_descriptionStart} signature field should be base64 encoded. Actual : ${data.signature}`);
};






describe('=== JsonProtocolCheck ===', function() {
  it('should compile', function () {
    assertChai.equal(1 + 1, 2, 'JsonProtocolChecker.js should compile');
  });
});
