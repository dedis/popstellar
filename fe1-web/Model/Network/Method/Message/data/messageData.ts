
/** Enumeration of all possible "object" field values in MessageData */
export enum ObjectType {
    // uninitialized placeholder
    INVALID = '__INVALID_OBJECT__',

    LAO = 'lao',
    MESSAGE = 'message',
    MEETING = 'meeting',
    ROLL_CALL = 'roll_call',
}

/** Enumeration of all possible "action" field values in MessageData */
export enum ActionType {
    // uninitialized placeholder
    INVALID = '__INVALID_ACTION__',

    CREATE = 'create',
    UPDATE_PROPERTIES = 'update_properties',
    STATE = 'state',
    WITNESS = 'witness',
    OPEN = 'open',
    REOPEN = 'reopen',
    CLOSE = 'close',
}

export interface MessageData {
    readonly object: ObjectType;
    readonly action: ActionType;
}