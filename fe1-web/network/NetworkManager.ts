import { JsonRpcRequest, JsonRpcResponse } from 'model/network';
import { NetworkConnection } from './NetworkConnection';
import { NetworkError } from './NetworkError';

let NETWORK_MANAGER_INSTANCE: NetworkManager;

class NetworkManager {
  private connections: NetworkConnection[];

  public constructor() {
    this.connections = [];
  }

  private static buildAddress(uri: string, port: number, path: string): string {
    return `ws://${uri}:${port}${(path === '') ? '' : `/${path}`}`;
  }

  private getConnectionByAddress(address: string): NetworkConnection | undefined {
    return this.connections.find((nc: NetworkConnection) => nc.address === address);
  }

  public connect(uri: string, port: number = 8000, path: string = ''): NetworkConnection {
    const address: string = NetworkManager.buildAddress(uri, port, path);
    if (this.getConnectionByAddress(address) !== undefined) {
      throw new NetworkError(`A websocket connection towards '${address}' is already opened`);
    }

    const connection: NetworkConnection = new NetworkConnection(address);

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

  public disconnectFrom(uri: string, port: number = 8000, path: string = ''): void {
    const address: string = NetworkManager.buildAddress(uri, port, path);
    const connection = this.connections.find((nc: NetworkConnection) => nc.address === address);
    if (connection !== undefined) this.disconnect(connection);
  }

  public disconnectFromAll(): void {
    this.connections.forEach((nc: NetworkConnection) => nc.disconnect());
    this.connections = [];
  }

  public sendPayload(payload: JsonRpcRequest): Promise<JsonRpcResponse> {
    // For now, we only have 1 connection opened at a time: the organizer server
    return (this.connections.length)
      ? this.connections[0].sendPayload(payload)
      : (() => { throw new NetworkError('Cannot send payload : no websocket connection available'); })();
  }
}

export function getNetworkManager(): NetworkManager {
  if (NETWORK_MANAGER_INSTANCE === undefined) { NETWORK_MANAGER_INSTANCE = new NetworkManager(); }
  return NETWORK_MANAGER_INSTANCE;
}
