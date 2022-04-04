import { ProtocolError } from 'core/objects';

import { JsonRpcRequest } from './JsonRpcRequest';

export class ExtendedJsonRpcRequest {
  public readonly receivedFrom: string;

  public readonly request: JsonRpcRequest;

  constructor(extendedRequest: Partial<ExtendedJsonRpcRequest>, request: Partial<JsonRpcRequest>) {
    this.request = new JsonRpcRequest(request);

    if (extendedRequest.receivedFrom === undefined) {
      throw new ProtocolError(
        'An extended JsonRpcRequest must contain the address it was received from',
      );
    }
    this.receivedFrom = extendedRequest.receivedFrom;
  }
}
