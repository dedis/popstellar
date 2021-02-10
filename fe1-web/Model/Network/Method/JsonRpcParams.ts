import { Channel } from 'Model/Objects/Channel';
import { ProtocolError } from '../ProtocolError';
import { Verifiable } from '../Verifiable';
import { Message } from './Message/Message';

export class JsonRpcParams implements Verifiable {

    public readonly channel: Channel;

    constructor(params: Partial<JsonRpcParams>) {
        if (params.channel === undefined) {
            throw new ProtocolError("Undefined 'channel' parameter in JSON-RPC");
        } else if (params.channel.length === 0) {
            throw new ProtocolError("Empty 'channel' parameter in JSON-RPC");
        }

        this.channel = params.channel;
    }

    public verify(): boolean {
        // to be implemented ?
        return true;
    }
}

export class JsonRpcParamsWithMessage extends JsonRpcParams {

    public readonly message: Message;

    constructor(params: Partial<JsonRpcParamsWithMessage>) {
        super(params);

        if (params.message === undefined || params.message === null) {
            throw new ProtocolError("Undefined 'message' parameter in JSON-RPC");
        }

        this.message = new Message(params.message);
    }

    public verify(): boolean {
        return super.verify()
            && this.message.verify();
    }
}
