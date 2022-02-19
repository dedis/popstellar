import { KeyPairRegistry } from 'core/keypair/KeyPairRegistry';
import { SignatureType } from 'core/network/jsonrpc/messages';
import { getCurrentPopTokenFromStore } from 'features/wallet/objects';

/**
 * Configures the network callbacks in a KeyPairRegistry.
 *
 * @param registry - The KeyPairRegistry where we want to add the mapping
 */
export function configureNetwork(registry: KeyPairRegistry) {
  registry.add(SignatureType.POP_TOKEN, getCurrentPopTokenFromStore);
}
