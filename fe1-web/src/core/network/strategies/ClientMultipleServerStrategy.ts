import { ExtendedJsonRpcResponse, JsonRpcRequest } from 'core/network/jsonrpc';
import { NetworkConnection } from 'core/network/NetworkConnection';

export type SendingStrategy = (
  payload: JsonRpcRequest,
  connections: NetworkConnection[],
) => Promise<ExtendedJsonRpcResponse[]>;
