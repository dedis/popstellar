import { ObjectType, ActionType, MessageData } from './messageData';
import { CreateLao } from './lao/createLao';
import { Base64Data } from 'Model/Objects';

export function encodeMessageData(msgData: MessageData) : Base64Data {
    let data = JSON.stringify(msgData);
    return Base64Data.encode(data);
}

export function buildMessageData(msgData: MessageData) : MessageData {

    switch(msgData.object) {
        case ObjectType.LAO:
            return buildLaoMessage(msgData);

        case ObjectType.MEETING:
        case ObjectType.MESSAGE:
        case ObjectType.ROLL_CALL:
        default:
            // not yet implemented
            throw new Error("Not yet implemented");
    }
}

function buildLaoMessage(msgData: MessageData) : MessageData {
    switch(msgData.action) {
        case ActionType.CREATE:
            return new CreateLao(msgData);
        default:
            throw new Error("Not yet implemented");
    }
}