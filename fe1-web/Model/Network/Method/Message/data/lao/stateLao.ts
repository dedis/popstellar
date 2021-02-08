import { Hash } from "../../../../../Objects/hash";
import { PublicKey } from "../../../../../Objects/publicKey";
import { Timestamp } from "../../../../../Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { WitnessSignature } from "../../../../../Objects/witnessSignature";

export class StateLao implements MessageData {

    public readonly object: ObjectType;
    public readonly action: ActionType;

    public readonly id: Hash;
    public readonly name: string;
    public readonly creation: Timestamp;
    public readonly last_modified: Timestamp;
    public readonly organizer: PublicKey;
    public readonly witnesses: PublicKey[];
    public readonly modification_id: Hash;
    public readonly modification_signatures: WitnessSignature[];

    constructor(msg: Partial<StateLao>) {
        Object.assign(this, msg);
    }
}
