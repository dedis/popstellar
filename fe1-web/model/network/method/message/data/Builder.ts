import { Base64Data } from 'model/objects';
import { ActionType, MessageData, ObjectType } from './MessageData';
import { CreateLao, StateLao, UpdateLao } from './lao';
import { CreateMeeting, StateMeeting } from './meeting';
import {
  CloseRollCall, CreateRollCall, OpenRollCall, ReopenRollCall,
} from './rollCall';
import { WitnessMessage } from './witness';

export function encodeMessageData(msgData: MessageData): Base64Data {
  const data = JSON.stringify(msgData);
  return Base64Data.encode(data);
}

function buildLaoMessage(msgData: MessageData): MessageData {
  switch (msgData.action) {
    case ActionType.CREATE:
      return CreateLao.fromJson(msgData);
    case ActionType.UPDATE_PROPERTIES:
      return UpdateLao.fromJson(msgData);
    case ActionType.STATE:
      return StateLao.fromJson(msgData);
    default:
      throw new Error(`Unknown action '${msgData.action}' encountered while creating a LAO MessageData`);
  }
}

function buildMeetingMessage(msgData: MessageData): MessageData {
  switch (msgData.action) {
    case ActionType.CREATE:
      return CreateMeeting.fromJson(msgData);
    case ActionType.STATE:
      return StateMeeting.fromJson(msgData);
    default:
      throw new Error(`Unknown action '${msgData.action}' encountered while creating a meeting MessageData`);
  }
}

function buildRollCallMessage(msgData: MessageData): MessageData {
  switch (msgData.action) {
    case ActionType.CREATE:
      return CreateRollCall.fromJson(msgData);
    case ActionType.OPEN:
      return OpenRollCall.fromJson(msgData);
    case ActionType.REOPEN:
      return ReopenRollCall.fromJson(msgData);
    case ActionType.CLOSE:
      return CloseRollCall.fromJson(msgData);
    default:
      throw new Error(`Unknown action '${msgData.action}' encountered while creating a roll call MessageData`);
  }
}

function buildWitnessMessage(msgData: MessageData): MessageData {
  return WitnessMessage.fromJson(msgData);
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
