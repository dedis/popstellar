import { ProtocolError } from 'model/objects/ProtocolError';
import { JsonRpcParams } from './method/JsonRpcParams';
import { JsonRpcMethod } from './JsonRpcMethods';
import { Broadcast, Catchup, Publish, Subscribe, Unsubscribe } from './method';
import { validateJsonRpcRequest } from './validation';

/**
 * This class represents a JSON-RPC 2.0 Request (or Notification)
 */
export class JsonRpcRequest {
  public readonly jsonrpc: string;

  public readonly method: JsonRpcMethod;

  public readonly id?: number;

  public readonly params: JsonRpcParams;

  constructor(req: Partial<JsonRpcRequest>) {
    if (!req.method) {
      throw new ProtocolError("Undefined 'method' in JSON-RPC");
    }
    if (req.params === undefined || req.params === null) {
      throw new ProtocolError("Undefined 'params' in JSON-RPC");
    }

    switch (req.method) {
      // notification methods, expect no ID
      case JsonRpcMethod.BROADCAST:
        if (req.id !== undefined) {
          throw new ProtocolError("Found 'id' parameter in JSON-RPC notification");
        }
        break;

      // RPC methods, expect an ID
      case JsonRpcMethod.PUBLISH:
      case JsonRpcMethod.SUBSCRIBE:
      case JsonRpcMethod.UNSUBSCRIBE:
      case JsonRpcMethod.CATCHUP:
        if (req.id === undefined) {
          throw new ProtocolError("Undefined 'id' parameter in JSON-RPC request");
        }
        break;

      // Unsupported methods
      default:
        throw new ProtocolError(
          `Unrecognized method '${req.method}' encountered in JSON-RPC request`,
        );
    }

    this.method = req.method;
    this.id = req.id === undefined || req.id === null ? undefined : req.id;
    this.params = req.params;
    this.jsonrpc = '2.0';
  }

  static fromJson(jsonString: string): JsonRpcRequest {
    const obj = JSON.parse(jsonString);
    const { errors } = validateJsonRpcRequest(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid JSON-RPC request\n\n${errors}`);
    }

    return new JsonRpcRequest({
      ...obj,
      params: JsonRpcRequest.parseParams(obj.method, obj.params),
    });
  }

  private static parseParams(method: JsonRpcMethod, params: any): JsonRpcParams {
    switch (method) {
      case JsonRpcMethod.BROADCAST:
        return Broadcast.fromJson(params);
      case JsonRpcMethod.PUBLISH:
        return Publish.fromJson(params);
      case JsonRpcMethod.SUBSCRIBE:
        return Subscribe.fromJson(params);
      case JsonRpcMethod.UNSUBSCRIBE:
        return Unsubscribe.fromJson(params);
      case JsonRpcMethod.CATCHUP:
        return Catchup.fromJson(params);
      default:
        throw new ProtocolError('Unsupported method in JSON-RPC');
    }
  }
}
