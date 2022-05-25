import { OmitMethods } from 'core/types';

import { Hash } from '../../../core/objects/Hash';
import { PublicKey } from '../../../core/objects/PublicKey';

export type ServerAddress = string;

export interface ServerState {
  laoId: string;
  address: string;
  serverPublicKey: string;
  frontendPublicKey: string;
}

export class Server {
  /**
   * The lao this server is associated to
   */
  laoId: Hash;

  /**
   * The canonical address of the server
   */
  address: ServerAddress;

  /**
   * The public key of the server that can be used to send encrypted messages
   */
  serverPublicKey: PublicKey;

  /**
   * The public key of the server that can be used to send encrypted messages
   */
  frontendPublicKey: PublicKey;

  // NOTE: There is no need to store peers: ServerAddress[] here.
  // As soon as a greeting message arrives, we connect to all peers. The server addresses
  // will be added to the lao state as soon as a lao creation message is received
  // over each connection

  /**
   * Constructs a new server instance
   * @param server The properties of the new server instance
   */
  constructor(server: OmitMethods<Server>) {
    if (server.laoId === undefined) {
      throw new Error("Undefined 'laoId' when creating 'Server'");
    }
    this.laoId = server.laoId;

    if (server.address === undefined) {
      throw new Error("Undefined 'address' when creating 'Server'");
    }
    this.address = server.address;

    if (server.serverPublicKey === undefined) {
      throw new Error("Undefined 'serverPublicKey' when creating 'Server'");
    }
    this.serverPublicKey = server.serverPublicKey;

    if (server.frontendPublicKey === undefined) {
      throw new Error("Undefined 'frontendPublicKey' when creating 'Server'");
    }
    this.frontendPublicKey = server.frontendPublicKey;
  }

  /**
   * Deserializes a server object
   * @param server The serialized server data
   * @returns A deserialized server instance
   */
  public static fromState(server: ServerState): Server {
    return new Server({
      laoId: new Hash(server.laoId),
      address: server.address,
      serverPublicKey: new PublicKey(server.serverPublicKey),
      frontendPublicKey: new PublicKey(server.frontendPublicKey),
    });
  }

  /**
   * Serializes a server instance
   * @returns Serialized server data
   */
  public toState(): ServerState {
    return JSON.parse(JSON.stringify(this));
  }
}
