import { ActionType, ObjectType } from './MessageData';

/**
 * Interface to describe the properties of a certain type of message.
 *
 * @remarks
 * This has to be updated each time we want to add a new property of the messages.
 */
export interface MessageProperties {
  readonly isPopTokenSigned: boolean;
}

/**
 * Map to know the properties of the type of message we're going to send, according to the object
 * and the action.
 *
 * @remarks
 * This map has to be updated for each new kind of message, and each new properties we would like
 * to add.
 */
export const messagePropertiesMap: Map<ObjectType, Map<ActionType, MessageProperties>> = new Map([
  [ObjectType.CHIRP, new Map<ActionType, MessageProperties>([
    [ActionType.ADD, { isPopTokenSigned: true }],
    [ActionType.NOTIFY_ADD, { isPopTokenSigned: false }],
  ])],
  [ObjectType.ELECTION, new Map<ActionType, MessageProperties>([
    [ActionType.CAST_VOTE, { isPopTokenSigned: true }],
    [ActionType.RESULT, { isPopTokenSigned: false }],
    [ActionType.END, { isPopTokenSigned: false }],
    [ActionType.SETUP, { isPopTokenSigned: false }],
  ])],
  [ObjectType.LAO, new Map<ActionType, MessageProperties>([
    [ActionType.CREATE, { isPopTokenSigned: false }],
    [ActionType.STATE, { isPopTokenSigned: false }],
    [ActionType.UPDATE_PROPERTIES, { isPopTokenSigned: false }],
  ])],
  [ObjectType.MEETING, new Map<ActionType, MessageProperties>([
    [ActionType.CREATE, { isPopTokenSigned: false }],
    [ActionType.STATE, { isPopTokenSigned: false }],
  ])],
  [ObjectType.ROLL_CALL, new Map<ActionType, MessageProperties>([
    [ActionType.CLOSE, { isPopTokenSigned: false }],
    [ActionType.CREATE, { isPopTokenSigned: false }],
    [ActionType.OPEN, { isPopTokenSigned: false }],
    [ActionType.REOPEN, { isPopTokenSigned: false }],
  ])],
  [ObjectType.MESSAGE, new Map<ActionType, MessageProperties>([
    [ActionType.WITNESS, { isPopTokenSigned: false }],
  ])],
]);
