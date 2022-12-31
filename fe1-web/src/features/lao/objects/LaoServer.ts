import { Hash, HashState } from 'core/objects/Hash';
import { PublicKey, PublicKeyState } from 'core/objects/PublicKey';
import { OmitMethods } from 'core/types';

export type ServerAddress = string;

export interface LaoServerState {
  laoId: HashState;
  address: string;
  serverPublicKey: PublicKeyState;
  frontendPublicKey: PublicKeyState;
}

export class LaoServer {
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
  constructor(server: OmitMethods<LaoServer>) {
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
  public static fromState(server: LaoServerState): LaoServer {
    return new LaoServer({
      laoId: Hash.fromState(server.laoId),
      address: server.address,
      serverPublicKey: PublicKey.fromState(server.serverPublicKey),
      frontendPublicKey: PublicKey.fromState(server.frontendPublicKey),
    });
  }

  /**
   * Serializes a server instance
   * @returns Serialized server data
   */
  public toState(): LaoServerState {
    return {
      laoId: this.laoId.toState(),
      address: this.address,
      serverPublicKey: this.serverPublicKey.toState(),
      frontendPublicKey: this.frontendPublicKey.toState(),
    };
  }
}
