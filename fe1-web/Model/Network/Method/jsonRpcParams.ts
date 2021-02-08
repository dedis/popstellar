import { Message } from './Message/message';

export class JsonRpcParams {

    public readonly channel: string;

    constructor(channel: string) {
        this.channel = channel;
    }

    public verify(): boolean {
        // basic verification, could be improved
        return this.channel.length > 0;
    }
}

export class JsonRpcParamsWithMessage extends JsonRpcParams {

    public readonly message: Message;

    constructor(channel: string, message: Partial<Message>)
    {
        super(channel);
        this.message = new Message(message);
    }

    public verify(): boolean {
        return super.verify()
            && this.message
            && this.message.verify();
    }
}