import { Hash } from "Model/Objects/hash";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { PublicKey } from "Model/Objects/publicKey";

export class CloseRollCall implements MessageData {

    public readonly object: ObjectType = ObjectType.ROLL_CALL;
    public readonly action: ActionType = ActionType.CLOSE;

    public readonly id: Hash;
    public readonly start: Timestamp;
    public readonly end: Timestamp;
    public readonly attendees: PublicKey[];

    constructor(msg: Partial<CloseRollCall>) {
        Object.assign(this, msg);
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
