import { Hash } from "Model/Objects/hash";
import { PublicKey } from "Model/Objects/publicKey";
import { Timestamp } from "Model/Objects/timestamp";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { WitnessSignature } from "Model/Objects/witnessSignature";

export class StateLao implements MessageData {

    public readonly object: ObjectType = ObjectType.LAO;
    public readonly action: ActionType = ActionType.STATE;

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

    public verify(): boolean {
        // to be implemented...
        return true;
    }
}
