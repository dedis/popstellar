/**
 * The network module contains all the necessary code to communicate
 * with the backend systems, and abstracts away the intricacies of
 * network interactions from the rest of the application.
 *
 * This module specifically excludes the application-level parsing
 * of incoming messages, a task dealt with in the ingestion module.
 */

export * from './CommunicationApi';
export * from './JsonRpcApi';
export { getNetworkManager } from './NetworkManager';
export * from './NetworkError';
export * from './RpcOperationError';
export { configureNetwork } from './Configure';
