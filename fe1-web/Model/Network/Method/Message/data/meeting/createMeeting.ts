import { Hash } from "Model/Objects/hash";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class CreateMeeting implements MessageData {

    public readonly object: ObjectType = ObjectType.MEETING;
    public readonly action: ActionType = ActionType.CREATE;

    public readonly id: Hash;
    public readonly name: string;
    public readonly creation: Timestamp;
    public readonly location?: string;
    public readonly start: Timestamp;
    public readonly end?: Timestamp;
    public readonly extra?: {};

    constructor(msg: Partial<CreateMeeting>) {
        Object.assign(this, msg);
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
