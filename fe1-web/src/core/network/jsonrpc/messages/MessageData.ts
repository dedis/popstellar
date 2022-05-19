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
  REACTION = 'reaction',
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
  NOTIFY_ADD = 'notify_add',
  DELETE = 'delete',
  NOTIFY_DELETE = 'notify_delete',
  GREET = 'greet',
}

/** Enumeration of all possible signatures of a message */
export enum SignatureType {
  KEYPAIR = 'keypair',
  POP_TOKEN = 'pop_token',
}

export interface MessageData {
  readonly object: ObjectType;
  readonly action: ActionType;
}
