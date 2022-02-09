import { ActionType, ObjectType, SignatureType } from './MessageData';

/**
 * Map to know the properties of the type of message we're going to send, according to the object
 * and the action.
 *
 * @remarks
 * This map has to be updated for each new kind of message, and each new properties we would like
 * to add.
 */
export const messagePropertiesMap: Map<ObjectType, Map<ActionType, SignatureType>> = new Map([
  [ObjectType.CHIRP, new Map<ActionType, SignatureType>([
    [ActionType.ADD, SignatureType.POP_TOKEN],
    [ActionType.NOTIFY_ADD, SignatureType.KEYPAIR],
    [ActionType.DELETE, SignatureType.POP_TOKEN],
    [ActionType.NOTIFY_DELETE, SignatureType.KEYPAIR],
  ])],
  [ObjectType.REACTION, new Map<ActionType, SignatureType>([
    [ActionType.ADD, SignatureType.POP_TOKEN],
  ])],
  [ObjectType.ELECTION, new Map<ActionType, SignatureType>([
    [ActionType.CAST_VOTE, SignatureType.POP_TOKEN],
    [ActionType.RESULT, SignatureType.KEYPAIR],
    [ActionType.END, SignatureType.KEYPAIR],
    [ActionType.SETUP, SignatureType.KEYPAIR],
  ])],
  [ObjectType.LAO, new Map<ActionType, SignatureType>([
    [ActionType.CREATE, SignatureType.KEYPAIR],
    [ActionType.STATE, SignatureType.KEYPAIR],
    [ActionType.UPDATE_PROPERTIES, SignatureType.KEYPAIR],
  ])],
  [ObjectType.MEETING, new Map<ActionType, SignatureType>([
    [ActionType.CREATE, SignatureType.KEYPAIR],
    [ActionType.STATE, SignatureType.KEYPAIR],
  ])],
  [ObjectType.ROLL_CALL, new Map<ActionType, SignatureType>([
    [ActionType.CLOSE, SignatureType.KEYPAIR],
    [ActionType.CREATE, SignatureType.KEYPAIR],
    [ActionType.OPEN, SignatureType.KEYPAIR],
    [ActionType.REOPEN, SignatureType.KEYPAIR],
  ])],
  [ObjectType.MESSAGE, new Map<ActionType, SignatureType>([
    [ActionType.WITNESS, SignatureType.KEYPAIR],
  ])],
]);
