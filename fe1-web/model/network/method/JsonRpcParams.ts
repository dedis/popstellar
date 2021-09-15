import { Channel } from 'model/objects/Channel';
import { ProtocolError } from '../ProtocolError';

/**
 * JSON-RPC Params containing:
 * - Channel
 */
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

  public static fromJson(obj: any): JsonRpcParams {
    // Schema validation already passed at top level
    return new JsonRpcParams({
      channel: obj.channel,
    });
  }
}
