// eslint-disable-next-line max-classes-per-file
import { ActionType, ObjectType } from './MessageData';

/**
 * Interface to describe the properties of a certain type of message.
 *
 * @remarks
 * For now, there are only 2 classes of messages: the ones that are signed using a token, and the
 * one that use the public key as a signature. (2021-12-27, Xelowak)
 */
export interface MessageProperties {
  readonly isPopTokenSigned: boolean;
}

class PopTokenSignedMessage implements MessageProperties {
  readonly isPopTokenSigned = true;
}

class PublicKeySignedMessage implements MessageProperties {
  readonly isPopTokenSigned = false;
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
    [ActionType.ADD, new PopTokenSignedMessage()],
    [ActionType.ADD_BROADCAST, new PublicKeySignedMessage()],
  ])],
  [ObjectType.ELECTION, new Map<ActionType, MessageProperties>([
    [ActionType.CAST_VOTE, new PopTokenSignedMessage()],
    [ActionType.RESULT, new PublicKeySignedMessage()],
    [ActionType.END, new PublicKeySignedMessage()],
    [ActionType.SETUP, new PublicKeySignedMessage()],
  ])],
  [ObjectType.LAO, new Map<ActionType, MessageProperties>([
    [ActionType.CREATE, new PublicKeySignedMessage()],
    [ActionType.STATE, new PublicKeySignedMessage()],
    [ActionType.UPDATE_PROPERTIES, new PublicKeySignedMessage()],
  ])],
  [ObjectType.MEETING, new Map<ActionType, MessageProperties>([
    [ActionType.CREATE, new PublicKeySignedMessage()],
    [ActionType.STATE, new PublicKeySignedMessage()],
  ])],
  [ObjectType.ROLL_CALL, new Map<ActionType, MessageProperties>([
    [ActionType.CLOSE, new PublicKeySignedMessage()],
    [ActionType.CREATE, new PublicKeySignedMessage()],
    [ActionType.OPEN, new PublicKeySignedMessage()],
    [ActionType.REOPEN, new PublicKeySignedMessage()],
  ])],
  [ObjectType.MESSAGE, new Map<ActionType, MessageProperties>([
    [ActionType.WITNESS, new PublicKeySignedMessage()],
  ])],
]);
