import { Hash } from "../../../../../Objects/hash";
import { Timestamp } from "../../../../../Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class CreateMeeting implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

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
}
