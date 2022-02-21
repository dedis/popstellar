import { JsonRpcRequest } from './jsonrpc';

export type JsonRpcHandler = (message: JsonRpcRequest) => void;

export const defaultRpcHandler: JsonRpcHandler = (message: JsonRpcRequest) =>
  console.warn('No RPC handler was provided to manage messages : ', message);
