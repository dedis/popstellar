import Ajv from 'ajv';
import addFormats from 'ajv-formats';
import jsonRPC from 'protocol/jsonRPC.json';
import connectToLaoSchema from 'protocol/qrcode/connect_to_lao.json';

import answerSchema from 'core/network/validation/schemas/answerSchemas';
import dataSchema from 'core/network/validation/schemas/dataSchemas';
import messageSchema from 'core/network/validation/schemas/messageSchemas';
import querySchema from 'core/network/validation/schemas/querySchemas';

import { ActionType, ObjectType } from '../jsonrpc/messages';

const ajv = new Ajv({ strict: false });
addFormats(ajv);
ajv.addSchema([
  jsonRPC,
  connectToLaoSchema,
  ...answerSchema,
  ...dataSchema,
  ...messageSchema,
  ...querySchema,
]);

const schemaPrefix = 'https://raw.githubusercontent.com/dedis/popstellar/master/protocol';

const schemaIds: Record<ObjectType, Record<string, string>> = {
  [ObjectType.INVALID]: {},
  [ObjectType.LAO]: {
    [ActionType.CREATE]: 'dataCreateLao',
    [ActionType.STATE]: 'dataStateLao',
    [ActionType.UPDATE_PROPERTIES]: 'dataUpdateLao',
    [ActionType.GREET]: 'dataGreetLao',
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
    [ActionType.OPEN]: 'dataOpenElection',
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
  [ObjectType.COIN]: {
    [ActionType.POST_TRANSACTION]: 'postTransaction',
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

export function validateDataObject(
  obj: ObjectType,
  action: ActionType,
  data: any,
): ValidationResult {
  const schemaId = getSchema(obj, action);
  return schemaId !== null
    ? validate(schemaId, data)
    : { errors: 'Unsupported data object - schema not found' };
}

export function validateConnectToLao(obj: any): ValidationResult {
  return validate(`${schemaPrefix}/qrcode/connect_to_lao.json`, obj);
}
