import { Hash } from './Hash';
import { PublicKey } from './PublicKey';
import { Signature } from './Signature';

export class WitnessSignature {
  public witness: PublicKey;

  public signature: Signature;

  constructor(witnessSignature: Partial<WitnessSignature>) {
    this.witness = witnessSignature.witness || (() => {
      throw new Error('WitnessSignature creation failed : missing witness');
    })();
    this.signature = witnessSignature.signature || (() => {
      throw new Error('WitnessSignature creation failed : missing signature');
    })();
  }

  /**
   * Verify the witness signature for the given message_id
   *
   * @param message_id to be verified
   */
  public verify(message_id: Hash) : boolean {
    return this.signature.verify(this.witness, message_id);
  }
}
