import { JsonRpcRequest, JsonRpcResponse } from 'core/network/jsonrpc';
import { NetworkConnection } from 'core/network/NetworkConnection';
import { NetworkError } from 'core/network/NetworkError';
import { defaultRpcHandler, JsonRpcHandler } from 'core/network/RpcHandler';

let NETWORK_MANAGER_INSTANCE: NetworkManager;

class NetworkManager {
  private connections: NetworkConnection[];

  private rpcHandler: JsonRpcHandler = defaultRpcHandler;

  public constructor() {
    this.connections = [];
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
   * @returns a Promise being resolved with the response,
   * or rejected with an error if the payload could not be delivered or was rejected by the server
   */
  public sendPayload(payload: JsonRpcRequest): Promise<JsonRpcResponse> {
    // For now, we only have 1 connection opened at a time: the organizer server

    if (this.connections.length === 0) {
      return Promise.reject(
        new NetworkError('Cannot send payload: no websocket connection available'),
      );
    }

    return this.connections[0].sendPayload(payload).catch((error) => {
      // Some day, we will want to retry from a different network connection,
      // before throwing an error to the caller
      console.error('Could not send payload due to failure:', error);
      throw error;
    });
  }

  public setRpcHandler(handler: JsonRpcHandler): void {
    this.rpcHandler = handler;
    this.connections.forEach((c) => c.setRpcHandler(handler));
  }
}

export function getNetworkManager(): NetworkManager {
  if (NETWORK_MANAGER_INSTANCE === undefined) {
    NETWORK_MANAGER_INSTANCE = new NetworkManager();
  }
  return NETWORK_MANAGER_INSTANCE;
}
