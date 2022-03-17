import { JsonRpcRequest, JsonRpcResponse } from './jsonrpc';
import { NetworkConnection } from './NetworkConnection';
import { defaultRpcHandler, JsonRpcHandler } from './RpcHandler';
import { SendingStrategy } from './strategies/client-multiple-servers/ClientMultipleServerStrategy';
import { sendToFirstAcceptingServerStrategy } from './strategies/client-multiple-servers/SendToFirstAcceptingServerStrategy';

let NETWORK_MANAGER_INSTANCE: NetworkManager;

class NetworkManager {
  private connections: NetworkConnection[];

  private rpcHandler: JsonRpcHandler = defaultRpcHandler;

  private sendingStrategy: SendingStrategy;

  public constructor(sendingStrategy: SendingStrategy) {
    this.connections = [];
    this.sendingStrategy = sendingStrategy;
  }

  private getConnectionByAddress(address: string): NetworkConnection | undefined {
    return this.connections.find((nc: NetworkConnection) => nc.address === address);
  }

  /** Connects to a server or returns an existing connection to the server
   * Mockserver port: 8080
   * Go backend default organizer port: 9000
   * The full path to connect to the backend is:
   * as organizer ws://host:clientport/organizer/client/
   * as a witness: ws://host:witnessport/organizer/witness/
   *
   * @param address the server's full address (URI)
   *
   * @returns a new connection to the server, or an existing one if it's already established
   */
  public connect(address: string): NetworkConnection {
    if (!address) {
      throw new Error('No address provided in connect');
    }

    const existingConnection = this.getConnectionByAddress(address);

    if (existingConnection !== undefined) {
      return existingConnection;
    }

    const connection: NetworkConnection = new NetworkConnection(address, this.rpcHandler);
    this.connections.push(connection);
    return connection;
  }

  public disconnect(connection: NetworkConnection): void {
    const index = this.connections.indexOf(connection);
    if (index !== -1) {
      this.connections[index].disconnect();
      this.connections.splice(index, 1);
    }
  }

  public disconnectFrom(address: string): void {
    if (!address) {
      throw new Error('No address provided in disconnectFrom');
    }
    const connection = this.getConnectionByAddress(address);
    if (connection !== undefined) {
      this.disconnect(connection);
    }
  }

  public disconnectFromAll(): void {
    this.connections.forEach((nc: NetworkConnection) => nc.disconnect());
    this.connections = [];
  }

  /** Sends a JsonRpcRequest over the network to any relevant server.
   *
   * @param payload The JsonRpcRequest you want to send
   *
   * @returns List of a promises being resolved with the responses,
   * or rejected with an error if the payload could not be delivered or was rejected by the server
   */
  public sendPayload(payload: JsonRpcRequest): Promise<JsonRpcResponse[]> {
    return this.sendingStrategy(payload, this.connections);
  }

  public setRpcHandler(handler: JsonRpcHandler): void {
    this.rpcHandler = handler;
    this.connections.forEach((c) => c.setRpcHandler(handler));
  }
}

export function getNetworkManager(): NetworkManager {
  if (NETWORK_MANAGER_INSTANCE === undefined) {
    // TODO: decide what the desired strategy is
    NETWORK_MANAGER_INSTANCE = new NetworkManager(sendToFirstAcceptingServerStrategy);
  }
  return NETWORK_MANAGER_INSTANCE;
}
