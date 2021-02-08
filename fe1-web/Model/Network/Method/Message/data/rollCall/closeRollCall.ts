import { Hash } from "../../../../../Objects/hash";
import { Timestamp } from "../../../../../Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { PublicKey } from "../../../../../Objects/publicKey";

export class CloseRollCall implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

    public readonly id: Hash;
    public readonly start: Timestamp;
    public readonly end: Timestamp;
    public readonly attendees: PublicKey[];

    constructor(msg: Partial<CloseRollCall>) {
        Object.assign(this, msg);
    }
}
