import { JsonRpcMethod, JsonRpcRequest } from '../jsonrpc';
import { JsonRpcParams } from '../jsonrpc/JsonRpcParams';

export const mockJsonRpcPayload = new JsonRpcRequest({
  id: 1,
  jsonrpc: '',
  method: JsonRpcMethod.PUBLISH,
  params: new JsonRpcParams({ channel: 'some channel' }),
});
