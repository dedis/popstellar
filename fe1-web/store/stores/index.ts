/**
 * The stores module contains functions whose API allows the static retrieval
 * of persisted data, outside the scope of React components.
 *
 * They should only be used as a last resort, for example where the code cannot
 * be linked directly with the storage through React-Redux.
 *
 * As such, these APIs are used among others during the processing of messages
 * received from the network, because those operations are disconnected from
 */

export * from './KeyPairStore';
export * from './OpenedLaoStore';
export * from './WalletStore';
export * from './EventStore'; // remove?
