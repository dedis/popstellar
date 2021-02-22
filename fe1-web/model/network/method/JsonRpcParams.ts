import { Channel } from 'model/objects/Channel';
import { ProtocolError } from '../ProtocolError';

export class JsonRpcParams {
  public readonly channel: Channel;

  constructor(params: Partial<JsonRpcParams>) {
    if (params.channel === undefined) {
      throw new ProtocolError("Undefined 'channel' parameter in JSON-RPC");
    } else if (params.channel.length === 0) {
      throw new ProtocolError("Empty 'channel' parameter in JSON-RPC");
    }

    this.channel = params.channel;
  }
}
