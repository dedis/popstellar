import { SignatureType } from 'core/network/jsonrpc/messages';
import { KeyPair, ProtocolError } from 'core/objects';

import { KeyPairStore } from './KeyPairStore';

type GetKeyPairFunc = () => Promise<KeyPair>;

const { KEYPAIR, POP_TOKEN } = SignatureType;

interface KeyPairEntry {
  // Function to get the corresponding key pair to sign the message accordingly
  getSignatureKeyPair?: GetKeyPairFunc;
}

/**
 * Registry of signature types and their corresponding function to get the key pair.
 */
export class KeyPairRegistry {
  private readonly mapping = new Map<string, KeyPairEntry>([
    [KEYPAIR, { getSignatureKeyPair: () => Promise.resolve(KeyPairStore.get()) }],
    [POP_TOKEN, {}],
  ]);

  /**
   * Adds an entry to the registry.
   *
   * @param signature - The type of signature of the message
   * @param getKeyPairFunc - The function to get the signing key pair of the message
   */
  add(signature: SignatureType, getKeyPairFunc: GetKeyPairFunc) {
    const entry = this.getEntry(signature);
    entry.getSignatureKeyPair = getKeyPairFunc;
  }

  /**
   * Gets the signature key pair of a type of signature.
   *
   * @param signature - The corresponding signature
   */
  async getSignatureKeyPair(signature: SignatureType): Promise<KeyPair> {
    const messageEntry = this.getEntry(signature);
    return messageEntry.getSignatureKeyPair!();
  }

  /**
   * Verifies that all properties of the KeyPairEntries of the registry are defined.
   *
   * @throws Error if a property is undefined
   *
   * @remarks This must be executed before starting the application.
   */
  verifyEntries() {
    for (const [key, entry] of this.mapping) {
      if (entry.getSignatureKeyPair === undefined) {
        throw new Error(
          `Signature '${key}' does not have a 'getSignatureKeyPair' property in KeyPairRegistry`,
        );
      }
    }
  }

  /**
   * Gets an entry of the registry.
   *
   * @param signature - The type of signature we want the entry
   * @throws Error if the signature is not in the registry
   *
   * @private
   */
  private getEntry(signature: SignatureType): KeyPairEntry {
    const keyPairEntry = this.mapping.get(signature);
    if (keyPairEntry === undefined) {
      throw new ProtocolError(`Signature '${signature}' is not contained in KeyPairRegistry`);
    }
    return keyPairEntry;
  }
}
