import { ExtendedJsonRpcRequest } from './jsonrpc';

export type JsonRpcHandler = (message: ExtendedJsonRpcRequest) => void;

export const defaultRpcHandler: JsonRpcHandler = (message: ExtendedJsonRpcRequest) =>
  console.warn('No RPC handler was provided to manage messages : ', message);
