import { JsonRpcParams } from './Method/JsonRpcParams';
import { JsonRpcMethod } from './jsonRpcMethods';
import { Broadcast, Catchup, Publish, Subscribe, Unsubscribe } from './Method';
import { ProtocolError } from './ProtocolError';

/**
 * This class represents a JSON-RPC 2.0 Request (or Notification)
 */
export class JsonRpcRequest {

    public readonly method: JsonRpcMethod;
    public readonly id?: number;
    public readonly params: JsonRpcParams;

    static fromJson(jsonString: string) : JsonRpcRequest {
        // validate with ajv (json-schema)

        let obj = JSON.parse(jsonString);

        return new JsonRpcRequest(obj as JsonRpcRequest);
    }

    constructor(req: Partial<JsonRpcRequest>) {
        Object.assign(this, req);

        if (!req.method) {
            throw new ProtocolError("Undefined 'method' in JSON-RPC");
        }
        if (req.params === undefined || req.params === null) {
            throw new ProtocolError("Undefined 'params' in JSON-RPC");
        }

        this.method = req.method;
        this.id = req.id || undefined;
        this.params = this._parseParams(req.params);
    }

    public verify(): boolean {
        switch(this.method) {
            // notification methods, expect no ID
            case JsonRpcMethod.BROADCAST:
                if ( this.id !== undefined ) {
                    return false;
                }
                break;

            // RPC methods, expect an ID
            case JsonRpcMethod.PUBLISH:
            case JsonRpcMethod.SUBSCRIBE:
            case JsonRpcMethod.UNSUBSCRIBE:
            case JsonRpcMethod.CATCHUP:
                if ( this.id === undefined ) {
                    return false;
                }
                break;

            // Unsupported methods
            default:
                return false;
        }

        return this.params.verify();
    }

    private _parseParams(params: Partial<JsonRpcParams>) : JsonRpcParams {
        switch(this.method) {
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
                throw new ProtocolError("Unsupported method in JSON-RPC");
        }
    }
}
