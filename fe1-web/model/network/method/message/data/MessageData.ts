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
  NOTIFY_ADD = 'add_broadcast',
  DELETE = 'delete',
  NOTIFY_DELETE = 'delete_broadcast',
}

export interface MessageData {
  readonly object: ObjectType;
  readonly action: ActionType;
}

/**
 * Returns if the message data is signed with token or not. If not, it has to be signed with
 * a public key.
 *
 * @remarks
 * This function has to be updated for each new kind of message that is going to be signed using
 * the pop token.
 *
 * @param data - The message data we want to know how to sign
 */
export function isSignedWithToken(data: MessageData): boolean {
  return (data.object === ObjectType.ELECTION && data.action === ActionType.CAST_VOTE)
    || (data.object === ObjectType.CHIRP && data.action === ActionType.ADD);
}
