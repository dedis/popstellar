import { ProtocolError } from 'core/objects';

import { JsonRpcResponse } from './JsonRpcResponse';

export class ExtendedJsonRpcResponse {
  public readonly receivedFrom: string;

  public readonly response: JsonRpcResponse;

  constructor(
    extendedResponse: Partial<ExtendedJsonRpcResponse>,
    response: Partial<JsonRpcResponse>,
  ) {
    this.response = new JsonRpcResponse(response);

    if (extendedResponse.receivedFrom === undefined) {
      throw new ProtocolError(
        'An extended JsonRpcResponse must contain the address it was received from',
      );
    }
    this.receivedFrom = extendedResponse.receivedFrom;
  }
}
