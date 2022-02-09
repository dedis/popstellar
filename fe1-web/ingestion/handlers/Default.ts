import { ExtendedMessage } from 'model/network/method/message';
import { MessageRegistry } from 'model/network/method/message/data';
import { configure } from './index';

// Create a registry and configure it
const registry = new MessageRegistry();
configure(registry);
console.log(registry);

/** Processes the messages from storage by dispatching them to the right handler
 *
 * @param msg a message
 *
 * @returns false if the message could not be handled
 * @returns true if the message was handled
 */
export function handleMessage(msg: ExtendedMessage): boolean {
  return registry.handleMessage(msg);
}
