import { JsonRpcRequest, JsonRpcResponse } from 'core/network/jsonrpc';
import { NetworkConnection } from 'core/network/NetworkConnection';

export type SendingStrategy = (
  payload: JsonRpcRequest,
  connections: NetworkConnection[],
) => Promise<JsonRpcResponse[]>;
