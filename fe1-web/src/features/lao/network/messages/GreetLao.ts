import { ActionType, MessageData, ObjectType } from 'core/network/jsonrpc/messages';
import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError, PublicKey } from 'core/objects';
import { ServerAddress } from 'features/lao/objects/Server';

/** Data received to track the state of a lao */
export class GreetLao implements MessageData {
  public readonly object: ObjectType = ObjectType.LAO;

  public readonly action: ActionType = ActionType.GREET;

  /**
   * The id of the lao this message is sent on
   */
  public readonly lao: Hash;

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
  public readonly peers: { address: ServerAddress }[];

  constructor(params: Partial<GreetLao>) {
    if (params.lao === undefined || params.lao === null) {
      throw new ProtocolError("Undefined 'lao' parameter in GreetLao");
    }
    this.lao = params.lao;

    if (params.sender === undefined || params.sender === null) {
      throw new ProtocolError("Undefined 'sender' parameter in GreetLao");
    }

    this.sender = params.sender;

    if (params.address === undefined || params.address === null) {
      throw new ProtocolError("Undefined 'address' parameter in GreetLao");
    }

    this.address = params.address;

    if (params.peers === undefined || params.peers === null || !Array.isArray(params.peers)) {
      throw new ProtocolError("Undefined 'peers' parameter in GreetLao");
    }

    // make sure .peers does not contain the address
    if (params.peers.find((peer) => peer.address === params.address)) {
      throw new ProtocolError("'peers' is not supposed to contain 'address' in GreetLao");
    }

    this.peers = params.peers;
  }

  public static fromJson(obj: any): GreetLao {
    const { errors } = validateDataObject(ObjectType.LAO, ActionType.GREET, obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid LAO greet\n\n${errors}`);
    }

    return new GreetLao({
      lao: new Hash(obj.lao),
      sender: obj.sender,
      address: obj.address,
      peers: obj.peers,
    });
  }
}
