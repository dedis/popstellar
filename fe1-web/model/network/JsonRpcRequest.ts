import { JsonRpcParams } from './method/JsonRpcParams';
import { JsonRpcMethod } from './JsonRpcMethods';
import {
  Broadcast, Catchup, Publish, Subscribe, Unsubscribe,
} from './method';
import { ProtocolError } from './ProtocolError';

/**
 * This class represents a JSON-RPC 2.0 Request (or Notification)
 */
export class JsonRpcRequest {
  public readonly method: JsonRpcMethod;

  public readonly id?: number;

  public readonly params: JsonRpcParams;

  constructor(req: Partial<JsonRpcRequest>) {
    Object.assign(this, req);

    if (!req.method) {
      throw new ProtocolError("Undefined 'method' in JSON-RPC");
    }
    if (req.params === undefined || req.params === null) {
      throw new ProtocolError("Undefined 'params' in JSON-RPC");
    }

    switch (req.method) {
      // notification methods, expect no ID
      case JsonRpcMethod.BROADCAST:
        if (this.id !== undefined) {
          throw new ProtocolError('Error: found \'id\' parameter during \'broadcast\' message creation');
        }
        break;

      // RPC methods, expect an ID
      case JsonRpcMethod.PUBLISH:
      case JsonRpcMethod.SUBSCRIBE:
      case JsonRpcMethod.UNSUBSCRIBE:
      case JsonRpcMethod.CATCHUP:
        if (this.id === undefined) {
          throw new ProtocolError('Undefined \'id\' parameter encountered during \'JsonRpcRequest\' creation');
        }
        break;

      // Unsupported methods
      default:
        throw new ProtocolError(`Unrecognized method '${req.method}' encountered during 'JsonRpcRequest' creation`);
    }

    this.method = req.method;
    this.id = req.id || undefined;
    this.params = this.parseParams(req.params);
  }

  static fromJson(jsonString: string) : JsonRpcRequest {
    // FIXME add JsonSchema validation to all "fromJson"
    const correctness = true;

    return correctness
      ? new JsonRpcRequest(JSON.parse(jsonString))
      : (() => { throw new ProtocolError('add JsonSchema error message'); })();
  }

  private parseParams(params: Partial<JsonRpcParams>) : JsonRpcParams {
    switch (this.method) {
      case JsonRpcMethod.BROADCAST:
        return new Broadcast(params);
      case JsonRpcMethod.PUBLISH:
        return new Publish(params);
      case JsonRpcMethod.SUBSCRIBE:
        return new Subscribe(params);
      case JsonRpcMethod.UNSUBSCRIBE:
        return new Unsubscribe(params);
      case JsonRpcMethod.CATCHUP:
        return new Catchup(params);
      default:
        throw new ProtocolError('Unsupported method in JSON-RPC');
    }
  }
}
