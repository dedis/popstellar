import { Hash } from "Model/Objects/hash";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { WitnessSignature } from "Model/Objects/witnessSignature";

export class StateMeeting implements MessageData {

    public readonly object: ObjectType = ObjectType.MEETING;
    public readonly action: ActionType = ActionType.STATE;

    public readonly id: Hash;
    public readonly name: string;
    public readonly creation: Timestamp;
    public readonly last_modified: Timestamp;
    public readonly location?: string;
    public readonly start: Timestamp;
    public readonly end?: Timestamp;
    public readonly extra?: {};
    public readonly modification_id: Hash;
    public readonly modification_signatures: WitnessSignature[];

    constructor(msg: Partial<StateMeeting>) {
        Object.assign(this, msg);
    }

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
