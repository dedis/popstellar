import Ajv from 'ajv';
import jsonRPC from 'protocol/jsonRPC.json';
import connectToLaoSchema from 'protocol/qrcode/connect_to_lao.json';
import querySchema from './querySchemas';
import messageSchema from './messageSchemas';
import dataSchema from './dataSchemas';
import answerSchema from './answerSchemas';

// FIXME: these two enums need to be redefined locally because otherwise their values are
//  undefined here, it could be due to cyclical dependencies that still need to be fixed.
//  C.f. https://github.com/kulshekhar/ts-jest/issues/281 and others

export enum ObjectType {
  // uninitialized placeholder
  INVALID = '__INVALID_OBJECT__',

  LAO = 'lao',
  MESSAGE = 'message',
  MEETING = 'meeting',
  ROLL_CALL = 'roll_call',
  ELECTION = 'election',
  CHIRP = 'chirp',
  REACTION = 'reaction',
}

export enum ActionType {
  // uninitialized placeholder
  INVALID = '__INVALID_ACTION__',

  CAST_VOTE = 'cast_vote',
  CREATE = 'create',
  END = 'end',
  SETUP = 'setup',
  UPDATE_PROPERTIES = 'update_properties',
  STATE = 'state',
  WITNESS = 'witness',
  OPEN = 'open',
  REOPEN = 'reopen',
  RESULT = 'result',
  CLOSE = 'close',
  ADD = 'add',
  NOTIFY_ADD = 'notify_add',
  DELETE = 'delete',
  NOTIFY_DELETE = 'notify_delete',
}

const ajv = new Ajv();
ajv.opts.strict = false;
ajv.addSchema([
  jsonRPC,
  connectToLaoSchema,
  ...answerSchema,
  ...dataSchema,
  ...messageSchema,
  ...querySchema,
]);

const schemaPrefix = 'https://raw.githubusercontent.com/dedis/student_21_pop/master/protocol';

const schemaIds: Record<ObjectType, Record<string, string>> = {
  [ObjectType.INVALID]: {},
  [ObjectType.LAO]: {
    [ActionType.CREATE]: 'dataCreateLao',
    [ActionType.STATE]: 'dataStateLao',
    [ActionType.UPDATE_PROPERTIES]: 'dataUpdateLao',
  },
  [ObjectType.MESSAGE]: {
    [ActionType.WITNESS]: 'dataWitnessMessage',
  },
  [ObjectType.MEETING]: {
    [ActionType.CREATE]: 'dataCreateMeeting',
    [ActionType.STATE]: 'dataStateMeeting',
  },
  [ObjectType.ROLL_CALL]: {
    [ActionType.CREATE]: 'dataCreateRollCall',
    [ActionType.OPEN]: 'dataOpenRollCall',
    [ActionType.REOPEN]: 'dataOpenRollCall',
    [ActionType.CLOSE]: 'dataCloseRollCall',
  },
  [ObjectType.ELECTION]: {
    [ActionType.SETUP]: 'dataSetupElection',
    [ActionType.CAST_VOTE]: 'dataCastVote',
    [ActionType.END]: 'dataEndElection',
    [ActionType.RESULT]: 'dataResultElection',
  },
  [ObjectType.CHIRP]: {
    [ActionType.ADD]: 'dataAddChirp',
    [ActionType.NOTIFY_ADD]: 'dataNotifyAddChirp',
    [ActionType.DELETE]: 'dataDeleteChirp',
    [ActionType.NOTIFY_DELETE]: 'dataNotifyDeleteChirp',
  },
  [ObjectType.REACTION]: {
    [ActionType.ADD]: 'dataAddReaction',
  },
};

function getSchema(obj: ObjectType, action: ActionType): string | null {
  if (obj in schemaIds && action in schemaIds[obj]) {
    return `${schemaPrefix}/query/method/message/data/${schemaIds[obj][action]}.json`;
  }
  return null;
}

export interface ValidationResult {
  errors: string | null;
}

function validate(schemaId: string, data: any): ValidationResult {
  const valid = ajv.validate(schemaId, data);
  return {
    errors: valid ? null : ajv.errorsText(ajv.errors),
  };
}

export function validateJsonRpcRequest(data: any): ValidationResult {
  const schemaId = `${schemaPrefix}/query/query.json`;
  return validate(schemaId, data);
}

export function validateJsonRpcResponse(data: any): ValidationResult {
  const schemaId = `${schemaPrefix}/answer/answer.json`;
  return validate(schemaId, data);
}

export function validateDataObject(obj: ObjectType, action: ActionType, data: any)
  : ValidationResult {
  const schemaId = getSchema(obj, action);
  return schemaId !== null
    ? validate(schemaId, data)
    : { errors: 'Unsupported data object - schema not found' };
}

export function validateConnectToLao(obj: any): ValidationResult {
  return validate(`${schemaPrefix}/qrcode/connect_to_lao.json`, obj);
}
