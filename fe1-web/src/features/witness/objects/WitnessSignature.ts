import { Hash, PublicKey, Signature } from 'core/objects';

/**
 * WitnessSignatureState is the interface that should match JSON.stringify(WitnessSignature)
 * It is used to store witness signatures in a way compatible with the Redux store.
 */
export interface WitnessSignatureState {
  witness: string;

  signature: string;
}

export class WitnessSignature {
  public witness: PublicKey;

  public signature: Signature;

  constructor(witnessSignature: Partial<WitnessSignature>) {
    this.witness =
      witnessSignature.witness ||
      (() => {
        throw new Error('WitnessSignature creation failed : missing witness');
      })();
    this.signature =
      witnessSignature.signature ||
      (() => {
        throw new Error('WitnessSignature creation failed : missing signature');
      })();
  }

  /**
   * Verify the witness signature for the given message_id
   *
   * @param message_id to be verified
   */
  public verify(message_id: Hash): boolean {
    return this.signature.verify(this.witness, message_id);
  }

  public static fromJson(ws: WitnessSignatureState): WitnessSignature {
    return new WitnessSignature({
      witness: new PublicKey(ws.witness),
      signature: new Signature(ws.signature),
    });
  }

  public toState(): WitnessSignatureState {
    return JSON.parse(JSON.stringify(this));
  }
}
