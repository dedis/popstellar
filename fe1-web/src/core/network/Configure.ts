import { KeyPairRegistry } from 'core/keypair';
import { MessageRegistry } from './jsonrpc/messages';
import { setSignatureKeyPair } from './JsonRpcApi';

/**
 * Configures the network with a MessageRegistry and a KeyPairRegistry.
 *
 * @param messageRegistry
 * @param keyPairRegistry
 */
export function configure(messageRegistry: MessageRegistry, keyPairRegistry: KeyPairRegistry) {
  setSignatureKeyPair(messageRegistry, keyPairRegistry);
}
