import { JsonRpcRequest } from 'core/network/jsonrpc';

export type JsonRpcHandler = (message: JsonRpcRequest) => void;

export function defaultRpcHandler(message: JsonRpcRequest) {
  console.warn('No RPC handler was provided to manage messages : ', message);
}
