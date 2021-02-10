import { Hash } from './hash';
import { PublicKey } from './publicKey';
import { Signature } from './signature';

export class WitnessSignature {
    public witness: PublicKey;
    public signature: Signature;

    constructor(witnessSignature: Partial<WitnessSignature>) {
        this.witness = witnessSignature.witness || (() => {
            throw new Error("WitnessSignature creation failed : missing witness")
        })();
        this.signature = witnessSignature.signature || (() => {
            throw new Error("WitnessSignature creation failed : missing signature")
        })();
    }

    /**
     * Verify the witness signature for the given message_id
     *
     * @param message_id to be verified
     */
    public verify(message_id: Hash) : boolean {
        this.signature.verify(this.witness, message_id);
        return false;
    }
}
