import { Hash, Signature } from "Model/Objects";
import { ActionType, MessageData, ObjectType } from "../messageData";
import { ProtocolError } from "../../../../ProtocolError";

export class WitnessMessage implements MessageData {

    public readonly object: ObjectType = ObjectType.MESSAGE;
    public readonly action: ActionType = ActionType.WITNESS;

    public readonly message_id: Hash;
    public readonly signature: Signature;

    constructor(msg: Partial<WitnessMessage>) {

        if (!msg.message_id) throw new ProtocolError('Undefined \'message_id\' parameter encountered during \'WitnessMessage\'');
        this.message_id = msg.message_id;

        if (!msg.signature) throw new ProtocolError('Undefined \'signature\' parameter encountered during \'WitnessMessage\'');
        this.signature = msg.signature;
    }

    verify(): boolean {
        return false;
    }

    public static fromJson(obj: any): WitnessMessage {
        throw new Error('Not implemented');
    }
}
