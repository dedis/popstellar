import { Hash } from './hash';
import { PublicKey } from './publicKey';
import { Signature } from './signature';

export class WitnessSignature {
    public witness: PublicKey;
    public signature: Signature;

    constructor(witnessSignature: Partial<WitnessSignature>) {
        Object.assign(this, witnessSignature);
    }

    /**
     * Verify the witness signature for the given message_id
     *
     * @param message_id to be verified
     */
    public verify(message_id: Hash) : boolean {
        // FIXME: verify that witness's signature over message_id is correct
        return false;
    }
}