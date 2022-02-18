/**
 * The network module contains all the necessary code to communicate
 * with the backend systems, and abstracts away the intricacies of
 * network interactions from the rest of the application.
 *
 * This module specifically excludes the application-level parsing
 * of incoming messages, a task dealt with in the ingestion module.
 */

import { MessageRegistry } from './jsonrpc/messages';

export { getNetworkManager } from './NetworkManager';
export * from './NetworkError';
export * from 'core/network/RpcOperationError';

export function configure(messageRegistry: MessageRegistry) {}
