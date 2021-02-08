import { Hash } from "../../../../../Objects/hash";
import { Timestamp } from "../../../../../Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { WitnessSignature } from "../../../../../Objects/witnessSignature";

export class StateMeeting implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

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
}
