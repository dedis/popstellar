import { Hash } from './Hash';
import { PublicKey, PublicKeyState } from './PublicKey';
import { Signature, SignatureState } from './Signature';

/**
 * WitnessSignatureState is the interface that should match JSON.stringify(WitnessSignature)
 * It is used to store witness signatures in a way compatible with the Redux store.
 */
export interface WitnessSignatureState {
  witness: PublicKeyState;
  signature: SignatureState;
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

  public static fromState(ws: WitnessSignatureState): WitnessSignature {
    return new WitnessSignature({
      witness: PublicKey.fromState(ws.witness),
      signature: Signature.fromState(ws.signature),
    });
  }

  public toState(): WitnessSignatureState {
    return {
      signature: this.signature.toState(),
      witness: this.witness.toState(),
    };
  }
}
