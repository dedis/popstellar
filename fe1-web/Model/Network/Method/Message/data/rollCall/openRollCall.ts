import { Hash } from "../../../../../Objects/hash";
import { Timestamp } from "../../../../../Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class OpenRollCall implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

    public readonly id: Hash;
    public readonly start: Timestamp;

    constructor(msg: Partial<OpenRollCall>) {
        Object.assign(this, msg);
    }
}
