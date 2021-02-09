import { MessageData } from './data/messageData';
import { buildMessageData } from './data/builder';
import { Verifiable } from 'Model/Network/verifiable';
import { WitnessSignature } from 'Model/Objects/witnessSignature';
import { PublicKey } from 'Model/Objects/publicKey';
import { Hash } from 'Model/Objects/hash';
import { Signature } from 'Model/Objects/signature';
import { Base64Data } from 'Model/Objects/base64';

export class Message implements Verifiable {
    public readonly data : Base64Data;
    public readonly sender : PublicKey;
    public readonly signature : Signature;
    public readonly message_id : Hash;
    public readonly witness_signatures : WitnessSignature[];

    private _messageData : MessageData;

    constructor(message: Partial<Message>) {
        Object.assign(this, message);

        let decoded : unknown = this.data.decode();
        this._messageData = buildMessageData(decoded as MessageData);
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