import { configureKeyPair } from 'core/keypair';
import { configureMessages } from 'core/network/jsonrpc/messages';
import { configureFeatures } from 'features';

export function configureTestFeatures() {
  configureKeyPair();
  const { messageRegistry } = configureFeatures();
  messageRegistry.verifyEntries();

  configureMessages(messageRegistry);

  return messageRegistry;
}
