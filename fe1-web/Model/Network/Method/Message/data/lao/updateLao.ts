import { Hash } from "../../../../../Objects/hash";
import { PublicKey } from "../../../../../Objects/publicKey";
import { Timestamp } from "../../../../../Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";

export class UpdateLao implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

    public readonly id: Hash;
    public readonly name: string;
    public readonly last_modified: Timestamp;
    public readonly witnesses: PublicKey[];

    constructor(msg: Partial<UpdateLao>) {
        Object.assign(this, msg);
    }
}
