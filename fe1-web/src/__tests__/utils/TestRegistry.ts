import { configureMessages } from 'core/network/jsonrpc/messages';
import { configureFeatures } from 'features';

export function configureTestMessageRegistry() {
  const { messageRegistry } = configureFeatures();
  messageRegistry.verifyEntries();

  configureMessages(messageRegistry);

  return messageRegistry;
}
