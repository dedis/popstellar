import 'jest-extended';
import * as wsUtils from 'network/WebsocketUtils';
import * as b64 from 'base-64';
import {
  mockCurrentLao,
  mockEventName,
  mockPublicKey,
  mockSecretKey,
  mockLocation,
} from '__tests__/network/MessageApi.test';

const QUERY_FIELD_COUNT = 4;
const ROOT_CHANNEL = '/root';
const METHODS = ['publish', 'subscribe', 'message', 'unsubscribe', 'catchup'];
const OBJECTS = ['lao'];
const ACTIONS = ['create', 'update_properties', 'state'];

/* ----------------------------------------- CHECK UTILS ---------------------------------------- */
const checkIsBase64String = (str) => {
  if (typeof str !== 'string') return false;
  try { b64.decode(str); } catch (error) { return false; }
  return true;
};

const checkArrayIsBase64 = (arr) => {
  const pubKeyLen = mockPublicKey.length;
  if (!Array.isArray(arr)) return false;

  const res = arr.filter((item) => (!checkIsBase64String(item) || item.length !== pubKeyLen));
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
  expect(typeof obj).toBe('object');
  expect(Object.keys(obj).length).toBe(QUERY_FIELD_COUNT);
  assertChai.hasAllKeys(obj, ['jsonrpc', 'method', 'params', 'id']);
  expect(obj.jsonrpc).toBe(wsUtils.JSON_RPC_VERSION);
  assertChai.oneOf(obj.method, METHODS, `unknown method: ${obj.method.toString()}`);
  expect(typeof obj.params).toBe('object');
  expect(typeof obj.id).toBe('number');
};

export const checkParams = (obj, isRoot = false) => {
  expect(typeof obj).toBe('object');
  assertChai.hasAllKeys(obj, ['channel', 'message']);
  expect(typeof obj.channel).toBe('string');
  expect(typeof obj.channel).toBe('string');
  if (isRoot) expect(obj.channel).toBe(ROOT_CHANNEL);
  else {
    expect(obj.channel).toMatch(/\/root\/[A-Za-z0-9+\/]*[=]*/);
    expect(checkIsBase64String(obj.channel.slice(ROOT_CHANNEL.length + 1))).toBe(true);
  }
  expect(typeof obj.message).toBe('object');
};

export const checkMessage = (obj) => {
  expect(typeof obj).toBe('object');
  assertChai.hasAllKeys(obj, ['data', 'sender', 'signature', 'message_id', 'witness_signatures']);

  expect(typeof obj.data).toBe('string');
  expect(checkIsBase64String(obj.data)).toBe(true);

  expect(typeof obj.sender).toBe('string');
  expect(checkIsBase64String(obj.sender)).toBe(true);
  expect(obj.sender).toBe(mockPublicKey);

  expect(typeof obj.signature).toBe('string');
  expect(checkIsBase64String(obj.signature)).toBe(true);
  const signExpected = wsUtils.signString(b64.decode(obj.data), mockSecretKey);
  expect(obj.signature).toBe(signExpected);

  expect(typeof obj.message_id).toBe('string');
  expect(checkIsBase64String(obj.message_id)).toBe(true);
  const hashExpected = wsUtils.hashStrings(obj.data, obj.signature);
  expect(obj.message_id).toBe(hashExpected);

  expect(Array.isArray(obj.witness_signatures)).toBe(true);
  expect(checkArrayKeySignPairIsBase64(obj.witness_signatures)).toBe(true);
};

/* --------------------------------- CHECK RANDOM LITTLE THINGS --------------------------------- */
export const checkQueryMethod = (request, method) => {
  expect(request.method).toBe(method);
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
  arr.forEach((e) => res += (`${e},`));
  return `[${res.slice(0, -1)}]`;
};

const _checkDataHeaders = (actualObject, expectedObject, storedObject, actualAction, expectedAction, storedAction) => {
  expect(typeof actualObject).toBe('string');
  expect(actualObject).toBe(expectedObject);
  expect(storedObject).toBe(expectedObject);

  expect(typeof expectedAction).toBe('string');
  expect(expectedAction).toBe(expectedAction);
  expect(storedAction).toBe(expectedAction);
};

export const checkQueryDataCreateLao = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedFields = _defaultDataFields.concat(['id', 'name', 'creation', 'organizer', 'witnesses']);
  assertChai.hasAllKeys(
    data,
    expectedFields,
    `${_descriptionStart} fields contains unknown/is missing some fields. Expected : ${_arrayToString(expectedFields)}`,
  );

  _checkDataHeaders(data.object, 'lao', object, data.action, 'create', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.name).toBe('string');
  expect(data.name).toBe(mockEventName);

  expect(typeof data.creation).toBe('number');
  expect(data.creation).toBeGreaterThan(0);

  expect(typeof data.organizer).toBe('string');
  expect(checkIsBase64String(data.organizer)).toBe(true);
  expect(data.organizer).toBe(mockPublicKey);

  expect(Array.isArray(data.witnesses)).toBe(true);
  expect(checkArrayIsBase64(data.witnesses)).toBe(true);
  expect(data.witnesses.length).toBe([...new Set(data.witnesses)].length);

  // check id
  expected = wsUtils.hashStrings(data.organizer, data.creation, data.name);
  expect(data.id).toBe(expected);
};

export const checkQueryDataUpdateLao = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedFields = _defaultDataFields.concat(['id', 'name', 'last_modified', 'witnesses']);
  assertChai.hasAllKeys(
    data,
    expectedFields,
    `${_descriptionStart} fields contains unknown/is missing some fields. Expected : ${_arrayToString(expectedFields)}`,
  );

  _checkDataHeaders(data.object, 'lao', object, data.action, 'update_properties', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.name).toBe('string');
  expect(data.name).toBe(mockEventName);

  expect(typeof data.last_modified).toBe('number');
  expect(data.last_modified).toBeGreaterThan(0);

  expect(Array.isArray(data.witnesses)).toBe(true);
  expect(checkArrayIsBase64(data.witnesses)).toBe(true);
  expect(data.witnesses.length).toBe([...new Set(data.witnesses)].length);

  // check id
  expected = wsUtils.hashStrings(mockCurrentLao.params.message.data.organizer, mockCurrentLao.params.message.data.creation, data.name);
  expect(data.id).toBe(expected);
};

export const checkQueryDataStateLao = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedFields = _defaultDataFields.concat(['id', 'name', 'creation', 'last_modified', 'organizer', 'witnesses', 'modification_id', 'modification_signatures']);
  assertChai.hasAllKeys(
    data,
    expectedFields,
    `${_descriptionStart} fields contains unknown/is missing some fields. Expected : ${_arrayToString(expectedFields)}`,
  );

  _checkDataHeaders(data.object, 'lao', object, data.action, 'state', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.name).toBe('string');
  expect(data.name).toBe(mockCurrentLao.params.message.data.name);

  expect(typeof data.creation).toBe('number');
  expect(data.creation).toBeGreaterThan(0);

  expect(typeof data.last_modified).toBe('number');
  expect(data.last_modified).toBeGreaterThan(0);
  expect(data.last_modified + 1).toBeGreaterThan(data.creation);

  expect(typeof data.organizer).toBe('string');
  expect(checkIsBase64String(data.organizer)).toBe(true);
  expect(data.organizer).toBe(mockPublicKey);

  expect(Array.isArray(data.witnesses)).toBe(true);
  expect(checkArrayIsBase64(data.witnesses)).toBe(true);
  expect(data.witnesses.length).toBe([...new Set(data.witnesses)].length);

  expect(typeof data.modification_id).toBe('string');
  expect(checkIsBase64String(data.modification_id)).toBe(true);

  expect(Array.isArray(data.modification_signatures)).toBe(true);
  expect(checkArrayKeySignPairIsBase64(data.modification_signatures)).toBe(true);

  // check id
  expected = wsUtils.hashStrings(data.organizer, data.creation, data.name);
  expect(data.id).toBe(expected);
};

export const checkQueryDataMeeting = (request, object, action) => {
  if (action === 'create') checkQueryDataCreateMeeting(request, object, action);
  else if (action === 'state') checkQueryDataStateMeeting(request, object, action);
  else expect(false).toBe(true);
};

export const checkQueryDataCreateMeeting = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedMinFields = _defaultDataFields.concat(['id', 'name', 'creation', 'start']);
  const optionalFields = ['location', 'end', 'extra'];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`,
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every((field) => possibleFields.includes(field));
  expect(isSubsetOfPossibleFields).toBe(true);

  _checkDataHeaders(data.object, 'meeting', object, data.action, 'create', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.name).toBe('string');
  expect(data.name).toBe(mockEventName);

  expect(typeof data.creation).toBe('number');
  expect(data.creation).toBeGreaterThan(0);

  if (Object.prototype.hasOwnProperty.call(data, 'location')) {
    expect(typeof data.location).toBe('string');
    expect(data.location).toBe(mockLocation);
  }

  expect(typeof data.start).toBe('number');
  expect(data.start).toBeGreaterThan(0);
  // assertChai.isAbove(data.start + 1, data.creation, `${_descriptionStart} start field should be greater than or equal to creation (${data.creation}) but is ${data.start}`);

  if (Object.prototype.hasOwnProperty.call(data, 'end')) {
    expect(typeof data.end).toBe('number');
    expect(data.end).toBeGreaterThan(0);
    expect(data.end + 1).toBeGreaterThan(data.start);
  }

  if (Object.prototype.hasOwnProperty.call(data, 'extra')) {
    expect(typeof data.extra).toBe('object');
  }

  // check id
  expected = wsUtils.hashStrings('M', mockCurrentLao.params.message.data.id, data.creation, data.name);
  expect(data.id).toBe(expected);
};

export const checkQueryDataStateMeeting = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedMinFields = _defaultDataFields.concat(['id', 'name', 'creation', 'last_modified', 'start', 'modification_id', 'modification_signatures']);
  const optionalFields = ['location', 'end', 'extra'];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`,
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every((field) => possibleFields.includes(field));
  expect(isSubsetOfPossibleFields).toBe(true);

  _checkDataHeaders(data.object, 'meeting', object, data.action, 'state', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.name).toBe('string');
  expect(data.name).toBe(mockCurrentLao.params.message.data.name);

  expect(typeof data.creation).toBe('number');
  expect(data.creation).toBeGreaterThan(0);

  expect(typeof data.last_modified).toBe('number');
  expect(data.last_modified).toBeGreaterThan(0);
  expect(data.last_modified + 1).toBeGreaterThan(data.creation);

  if (Object.prototype.hasOwnProperty.call(data, 'location')) {
    expect(typeof data.location).toBe('string');
    expect(data.location).toBe(mockLocation);
  }

  expect(typeof data.start).toBe('number');
  expect(data.start).toBeGreaterThan(0);
  expect(data.start + 1).toBeGreaterThan(data.creation);

  if (Object.prototype.hasOwnProperty.call(data, 'end')) {
    expect(typeof data.end).toBe('number');
    expect(data.end).toBeGreaterThan(0);
    expect(data.end + 1).toBeGreaterThan(data.start);
  }

  if (Object.prototype.hasOwnProperty.call(data, 'extra')) {
    expect(typeof data.extra).toBe('object');
  }

  expect(typeof data.modification_id).toBe('string');
  expect(checkIsBase64String(data.modification_id)).toBe(true);

  expect(Array.isArray(data.modification_signatures)).toBe(true);
  expect(checkArrayKeySignPairIsBase64(data.modification_signatures)).toBe(true);

  // check id
  expected = wsUtils.hashStrings('M', mockCurrentLao.params.message.data.id, data.creation, data.name);
  expect(data.id).toBe(expected);
};

export const checkQueryDataRollCall = (request, object, action) => {
  if (action === 'create') checkQueryDataCreateRollCall(request, object, action);
  else if (action === 'open') checkQueryDataOpenRollCall(request, object, action);
  else if (action === 'reopen') checkQueryDataReopenRollCall(request, object, action);
  else if (action === 'close') checkQueryDataCloseRollCall(request, object, action);
  else expect(false).toBe(true);
};

export const checkQueryDataCreateRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedMinFields = _defaultDataFields.concat(['id', 'name', 'creation', 'location']);
  const optionalFields = ['start', 'scheduled', 'roll_call_description'];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`,
  );
  const xor = (Object.prototype.hasOwnProperty.call(data, 'start') && Object.prototype.hasOwnProperty.call(data, 'scheduled'));
  expect(xor).toBe(false);
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every((field) => possibleFields.includes(field));
  expect(isSubsetOfPossibleFields).toBe(true);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'create', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.name).toBe('string');
  expect(data.name).toBe(mockEventName);

  expect(typeof data.creation).toBe('number');
  expect(data.creation).toBeGreaterThan(0);

  if (Object.prototype.hasOwnProperty.call(data, 'start')) {
    expect(typeof data.start).toBe('number');
    expect(data.start).toBeGreaterThan(0);
    // assertChai.isAbove(data.start + 1, data.creation, `${_descriptionStart} start field should be greater than or equal to creation (${data.creation}) but is ${data.start}`);
  }

  if (Object.prototype.hasOwnProperty.call(data, 'scheduled')) {
    expect(typeof data.scheduled).toBe('number');
    expect(data.scheduled).toBeGreaterThan(0);
    // assertChai.isAbove(data.scheduled + 1, data.creation, `${_descriptionStart} scheduled field should be greater than or equal to creation (${data.creation}) but is ${data.scheduled}`);
  }

  expect(typeof data.location).toBe('string');
  expect(data.location).toBe(mockLocation);

  if (Object.prototype.hasOwnProperty.call(data, 'roll_call_description')) {
    expect(typeof data.roll_call_description).toBe('string');
  }

  // check id
  expected = wsUtils.hashStrings('R', wsUtils.toString64(mockCurrentLao.params.message.data.id), data.creation, data.name);
  expect(data.id).toBe(expected);
};

export const checkQueryDataOpenRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedMinFields = _defaultDataFields.concat(['id', 'start']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`,
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every((field) => possibleFields.includes(field));
  expect(isSubsetOfPossibleFields).toBe(true);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'open', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.start).toBe('number');
  expect(data.start).toBeGreaterThan(0);

  // check id
  expected = wsUtils.hashStrings('R', mockCurrentLao.params.message.data.id, 444, 'r-cName'); // 444 and r-cName are for now hardocded in the APi
  expect(data.id).toBe(expected);
};

export const checkQueryDataReopenRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedMinFields = _defaultDataFields.concat(['id', 'start']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`,
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every((field) => possibleFields.includes(field));
  expect(isSubsetOfPossibleFields).toBe(true);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'reopen', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.start).toBe('number');
  expect(data.start).toBeGreaterThan(0);

  // check id
  expected = wsUtils.hashStrings('R', mockCurrentLao.params.message.data.id, 444, 'r-cName'); // 444 and r-cName are for now hardocded in the APi
  expect(data.id).toBe(expected);
};

export const checkQueryDataCloseRollCall = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedMinFields = _defaultDataFields.concat(['id', 'start', 'end', 'attendees']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  assertChai.containsAllKeys(
    data,
    expectedMinFields,
    `${_descriptionStart} fields are missing essential fields. Expected : ${_arrayToString(expectedMinFields)}`,
  );
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every((field) => possibleFields.includes(field));
  expect(isSubsetOfPossibleFields).toBe(true);

  _checkDataHeaders(data.object, 'roll_call', object, data.action, 'close', action);

  expect(typeof data.id).toBe('string');
  expect(checkIsBase64String(data.id)).toBe(true);

  expect(typeof data.start).toBe('number');
  expect(data.start).toBeGreaterThan(0);
  expect(data.start).toBe(555); // 555 is for now hardcoded in the API

  expect(typeof data.end).toBe('number');
  expect(data.end).toBeGreaterThan(0);
  expect(data.end + 1).toBeGreaterThan(data.start);

  expect(Array.isArray(data.attendees)).toBe(true);
  expect(checkArrayIsBase64(data.attendees)).toBe(true);
  expect(data.attendees.length).toBe([...new Set(data.attendees)].length);

  // check id
  expected = wsUtils.hashStrings('R', mockCurrentLao.params.message.data.id, 444, 'r-cName'); // 444 and r-cName are for now hardocded in the APi
  expect(data.id).toBe(expected);
};

export const checkQueryDataWitnessMessage = (request, object, action) => {
  const dataEncoded = request.params.message.data;
  let data;
  let expected;
  try {
    data = JSON.parse(b64.decode(dataEncoded));
  } catch (error) { expect(false).toBe(true); }

  expect(typeof data).toBe('object');
  const expectedMinFields = _defaultDataFields.concat(['message_id', 'signature']);
  const optionalFields = [];
  const possibleFields = expectedMinFields.concat(optionalFields);
  expect(data).toContainAllKeys(expectedMinFields);
  const isSubsetOfPossibleFields = Object.getOwnPropertyNames(data).every((field) => possibleFields.includes(field));
  expect(isSubsetOfPossibleFields).toBe(true);

  _checkDataHeaders(data.object, 'message', object, data.action, 'witness', action);

  expect(typeof data.message_id).toBe('string');
  expect(checkIsBase64String(data.message_id)).toBe(true);

  expect(typeof data.signature).toBe('string');
  expect(checkIsBase64String(data.signature)).toBe(true);
};
