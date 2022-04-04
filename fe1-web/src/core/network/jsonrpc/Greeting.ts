import { Channel, ProtocolError, PublicKey, ServerAddress } from 'core/objects';
import { RemoveMethods } from 'core/types';

import { JsonRpcParams } from './JsonRpcParams';

export class Greeting extends JsonRpcParams {
  /**
   * The channel this message is sent on
   */
  public readonly channel: Channel;

  /**
   * The public key of the server
   */
  public readonly sender: PublicKey;

  /**
   * The canonical address of the server with a protocol prefix
   */
  public readonly address: ServerAddress;

  /**
   * A list of peers the server is connected to (excluding itself)
   */
  public readonly peers: ServerAddress[];

  constructor(params: RemoveMethods<Greeting>) {
    super(params);

    if (params.channel === undefined || params.channel === null) {
      throw new ProtocolError("Undefined 'channel' parameter in Greeting");
    }
    this.channel = params.channel;

    if (params.sender === undefined || params.sender === null) {
      throw new ProtocolError("Undefined 'sender' parameter in Greeting");
    }

    this.sender = params.sender;

    if (params.address === undefined || params.address === null) {
      throw new ProtocolError("Undefined 'address' parameter in Greeting");
    }

    this.address = params.address;

    if (params.peers === undefined || params.peers === null || !Array.isArray(params.peers)) {
      throw new ProtocolError("Undefined 'peers' parameter in Greeting");
    }

    this.peers = params.peers;
  }

  public static fromJson(obj: any): Greeting {
    // Schema validation already passed at top level

    return new Greeting({
      channel: obj.channel,
      sender: obj.sender,
      address: obj.address,
      peers: obj.peers,
    });
  }
}
