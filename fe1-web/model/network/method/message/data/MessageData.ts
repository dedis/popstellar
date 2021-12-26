/** Enumeration of all possible "object" field values in MessageData */
export enum ObjectType {
  // uninitialized placeholder
  INVALID = '__INVALID_OBJECT__',

  LAO = 'lao',
  MESSAGE = 'message',
  MEETING = 'meeting',
  ROLL_CALL = 'roll_call',
  ELECTION = 'election',
  CHIRP = 'chirp',
}

/** Enumeration of all possible "action" field values in MessageData */
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
  ADD_BROADCAST = 'add_broadcast',
}

export interface MessageData {
  readonly object: ObjectType;
  readonly action: ActionType;
}

/**
 * Map to know if the type of message we're going to send needs to be signed using a token, or
 * the user's public key.
 *
 * @remarks
 * This map has to be updated for each new kind of message.
 */
export const isMessageSignedWithToken: Map<string, Map<string, boolean>> = new Map([
  [ObjectType.CHIRP, new Map([
    [ActionType.ADD, true],
    [ActionType.ADD_BROADCAST, false],
  ])],
  [ObjectType.ELECTION, new Map([
    [ActionType.CAST_VOTE, true],
    [ActionType.RESULT, false],
    [ActionType.END, false],
    [ActionType.SETUP, false],
  ])],
  [ObjectType.LAO, new Map([
    [ActionType.CREATE, false],
    [ActionType.STATE, false],
    [ActionType.UPDATE_PROPERTIES, false],
  ])],
  [ObjectType.MEETING, new Map([
    [ActionType.CREATE, false],
    [ActionType.STATE, false],
  ])],
  [ObjectType.ROLL_CALL, new Map([
    [ActionType.CLOSE, false],
    [ActionType.CREATE, false],
    [ActionType.OPEN, false],
    [ActionType.REOPEN, false],
  ])],
  [ObjectType.MESSAGE, new Map([
    [ActionType.WITNESS, false],
  ])],
]);
