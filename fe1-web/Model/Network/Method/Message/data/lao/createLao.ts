import { Hash } from "../../../../../Objects/hash";
import { PublicKey } from "../../../../../Objects/publicKey";
import { Timestamp } from "../../../../../Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class CreateLao implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

    public readonly id: Hash;
    public readonly name: string;
    public readonly creation: Timestamp;
    public readonly organizer: PublicKey;
    public readonly witnesses: PublicKey[];

    constructor(msg: Partial<CreateLao>) {
        Object.assign(this, msg);
    }
}