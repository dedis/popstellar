import { Base64Data } from 'Model/Objects';
import { ActionType, MessageData, ObjectType } from './messageData';
import { CreateLao, StateLao, UpdateLao } from './lao';
import { CreateMeeting, StateMeeting } from './meeting';
import { CloseRollCall, CreateRollCall, OpenRollCall } from './rollCall';
import { WitnessMessage } from './witness';


export function encodeMessageData(msgData: MessageData): Base64Data {
  const data = JSON.stringify(msgData);
  return Base64Data.encode(data);
}

function buildLaoMessage(msgData: MessageData): MessageData {
  switch (msgData.action) {
    case ActionType.CREATE:
      return new CreateLao(msgData);
    case ActionType.UPDATE_PROPERTIES:
      return new UpdateLao(msgData);
    case ActionType.STATE:
      return new StateLao(msgData);
    default:
      throw new Error(`Unknown action '${msgData.action}' encountered while creating a LAO MessageData`);
  }
}

function buildMeetingMessage(msgData: MessageData): MessageData {
  switch (msgData.action) {
    case ActionType.CREATE:
      return new CreateMeeting(msgData);
    case ActionType.STATE:
      return new StateMeeting(msgData);
    default:
      throw new Error(`Unknown action '${msgData.action}' encountered while creating a meeting MessageData`);
  }
}

function buildRollCallMessage(msgData: MessageData): MessageData {
  switch (msgData.action) {
    case ActionType.CREATE:
      return new CreateRollCall(msgData);
    case ActionType.OPEN:
    case ActionType.REOPEN:
      return new OpenRollCall(msgData);
    case ActionType.CLOSE:
      return new CloseRollCall(msgData);
    default:
      throw new Error(`Unknown action '${msgData.action}' encountered while creating a roll call MessageData`);
  }
}

function buildWitnessMessage(msgData: MessageData): MessageData {
  return new WitnessMessage(msgData);
}

export function buildMessageData(msgData: MessageData): MessageData {
  switch (msgData.object) {
    case ObjectType.LAO:
      return buildLaoMessage(msgData);

    case ObjectType.MEETING:
      return buildMeetingMessage(msgData);

    case ObjectType.MESSAGE:
      return buildWitnessMessage(msgData);

    case ObjectType.ROLL_CALL:
      return buildRollCallMessage(msgData);

    default:
      throw new Error(`Unknown object '${msgData.object}' encountered while creating a MessageData`);
  }
}
