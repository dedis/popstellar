import { Hash } from "Model/Objects/hash";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class CreateRollCall implements MessageData {

    public readonly object: ObjectType = ObjectType.ROLL_CALL;
    public readonly action: ActionType = ActionType.CREATE;

    public readonly id: Hash;
    public readonly name: string;
    public readonly creation: Timestamp;
    public readonly start?: Timestamp;
    public readonly scheduled?: Timestamp;
    public readonly location: string;
    public readonly roll_call_description?: string;

    constructor(msg: Partial<CreateRollCall>) {
        Object.assign(this, msg);
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
