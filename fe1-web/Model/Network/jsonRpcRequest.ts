import { JsonRpcParams } from './Method/jsonRpcParams';
import { JsonRpcMethod } from './jsonRpcMethods';
import { Verifiable } from './verifiable';


/*

let msgData = new CreateLao({
    ...
});

let message = Message.FromData(msgData);

let rpc = new JsonRpcRequest({
    method: 'publish',
    params: {
        channel: xxx,
        message: message,
    }
});



*/

export class JsonRpcRequest implements Verifiable {

    public readonly method: JsonRpcMethod;
    public readonly id?: number;
    public readonly params: JsonRpcParams;

    constructor(req: Partial<JsonRpcRequest>) {
        Object.assign(this, req);
        this.method = req.method || JsonRpcMethod.INVALID;
        this.id = req.id || undefined;
        if ( req.params ) {
            let rpcParams = this._parseParams(req.params);
            this.params = rpcParams || ;
        } else {
            // throw ?
        }
    }

    static fromJson(jsonString: string) : JsonRpcRequest {
        // validate with ajv (json-schema)
        return new JsonRpcRequest({});
    }

    public verify(): boolean {
        switch(this.method) {
            case JsonRpcMethod.INVALID:
                return false;

            case JsonRpcMethod.BROADCAST:
                // notification, expect no ID
                if ( this.id !== undefined ) {
                    return false;
                }
                break;

            default:
                // request, expect an ID
                if ( this.id === undefined ) {
                    return false;
                }
                break;
        }

        return this.params.verify();
    }

    private _parseParams(params: Partial<JsonRpcParams>) : JsonRpcParams | null {
        switch(this.method) {
            case JsonRpcMethod.BROADCAST:
                return null;
            case JsonRpcMethod.PUBLISH:
                return null;
            case JsonRpcMethod.SUBSCRIBE:
                return null;
            case JsonRpcMethod.UNSUBSCRIBE:
                return null;
            case JsonRpcMethod.CATCHUP:
                return null;
            default:
                return null;
        }
    }
}