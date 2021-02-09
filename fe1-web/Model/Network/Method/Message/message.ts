import { MessageData } from './data/messageData';
import { buildMessageData } from './data/builder';
import { Verifiable } from 'Model/Network/Verifiable';
import { ProtocolError } from 'Model/Network/ProtocolError';
import { Base64Data, Hash, PublicKey, Signature, WitnessSignature } from "Model/Objects";

export class Message implements Verifiable {
    public readonly data: Base64Data;
    public readonly sender: PublicKey;
    public readonly signature: Signature;
    public readonly message_id: Hash;
    public readonly witness_signatures: WitnessSignature[];

    public readonly messageData: MessageData;

    constructor(msg: Partial<Message>) {
        Object.assign(this, msg);

        let jsonData = this.data.decode();
        let dataObj = JSON.parse(jsonData);
        this.messageData = buildMessageData(dataObj as MessageData);
    }

    public verify(): boolean {
        let res = true;

        // verify sender's signature on data

        // verify witnesses' signature
        this.witness_signatures.forEach(witSig => {
            res = res && witSig.verify(this.message_id);
        });

        return res;
    }
}