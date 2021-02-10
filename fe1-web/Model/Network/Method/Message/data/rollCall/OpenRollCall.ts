import { Hash } from "Model/Objects/hash";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class OpenRollCall implements MessageData {

    public readonly object: ObjectType = ObjectType.ROLL_CALL;
    public readonly action: ActionType = ActionType.OPEN;

    public readonly id: Hash;
    public readonly start: Timestamp;

    constructor(msg: Partial<OpenRollCall>) {
        Object.assign(this, msg);
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
