import { validateJsonRpcResponse } from 'core/network/validation';
import { ProtocolError } from 'core/objects';

import { Message } from './messages';

export const UNDEFINED_ID: number = -1;

interface ErrorObject {
  code: number;
  description: string;
  data?: any; // part of JSON-RPC specification, unused in PoP for now.
}

export class JsonRpcResponse {
  public readonly jsonrpc: string;

  public readonly result?: number | Message[];

  public readonly error?: ErrorObject;

  public readonly id: number;

  constructor(resp: Partial<JsonRpcResponse>) {
    this.id = resp.id === undefined ? UNDEFINED_ID : resp.id;

    if (resp.error !== undefined && resp.result !== undefined) {
      throw new ProtocolError("Unexpected json-rpc answer : both 'error' and 'result' are present");
    }

    if (resp.result !== undefined) {
      if (typeof resp.result === 'number' && resp.result) {
        throw new ProtocolError('Unexpected json-rpc answer : non-zero result value');
      }

      this.result = resp.result;
    } else if (resp.error !== undefined) {
      if (resp.error.code >= 0) {
        throw new ProtocolError(
          `Unexpected json-rpc answer : unexpected error code value '${resp.error.code}'`,
        );
      }

      this.error = resp.error;
    } else {
      throw new ProtocolError("Unexpected json-rpc answer : both 'error' and 'result' are absent");
    }

    this.jsonrpc = '2.0';
  }

  public static fromJson(jsonString: string): JsonRpcResponse {
    const obj = JSON.parse(jsonString);
    const { errors } = validateJsonRpcResponse(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid JSON-RPC response\n\n${errors}`);
    }

    return new JsonRpcResponse(obj);
  }
}
