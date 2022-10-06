import NetInfo, { NetInfoState } from '@react-native-community/netinfo';
import { AppState, AppStateStatus } from 'react-native';

import { ExtendedJsonRpcResponse, JsonRpcRequest } from './jsonrpc';
import { NetworkConnection } from './NetworkConnection';
import { defaultRpcHandler, JsonRpcHandler } from './RpcHandler';
import { SendingStrategy } from './strategies/ClientMultipleServerStrategy';
import { sendToAllServersStrategy } from './strategies/SendToAllServersStrategy';

let NETWORK_MANAGER_INSTANCE: NetworkManager;

type ReconnectionHandler = () => void;
type ConnectionDeathHandler = (address: string) => void;

class NetworkManager {
  private connections: NetworkConnection[];

  private rpcHandler: JsonRpcHandler = defaultRpcHandler;

  private readonly sendingStrategy: SendingStrategy;

  private isOnline: boolean;

  private isFocused: boolean;

  private reconnectionHandlers: ReconnectionHandler[] = [];

  private connectionDeathHandlers: ConnectionDeathHandler[] = [];

  public constructor(sendingStrategy: SendingStrategy) {
    this.connections = [];
    this.sendingStrategy = sendingStrategy;
    this.isOnline = true;
    this.isFocused = true;

    // fetch initial network status
    NetInfo.fetch().then(this.onNetworkChange.bind(this));
    // listen to network change events
    NetInfo.addEventListener(this.onNetworkChange.bind(this));

    // reconnect when the application is re-focused
    AppState.addEventListener('change', this.onAppStateChange.bind(this));
  }

  private onNetworkChange(state: NetInfoState): void {
    const isOnline = state.isConnected || false;
    if (!this.isOnline && isOnline) {
      // if we were disconnected before and are now reconnected to the network
      // then try to reconnect all connections
      this.reconnectIfNecessary();
    }

    this.isOnline = isOnline;
  }

  private onAppStateChange(status: AppStateStatus) {
    const isFocused = status === 'active';
    if (!this.isFocused && isFocused) {
      // if we were backgrounded and become active again, the websocket
      // connections are very likely to be broken
      this.reconnectIfNecessary();
    }

    this.isFocused = isFocused;
  }

  public addReconnectionHandler(handler: ReconnectionHandler) {
    this.reconnectionHandlers.push(handler);
  }

  public removeReconnectionHandler(handler: ReconnectionHandler) {
    this.reconnectionHandlers = this.reconnectionHandlers.filter((h) => h !== handler);
  }

  public removeAllReconnectionHandler() {
    this.reconnectionHandlers = [];
  }

  public addConnectionDeathHandler(handler: ConnectionDeathHandler) {
    this.connectionDeathHandlers.push(handler);
  }

  public removeConnectionDeathHandler(handler: ConnectionDeathHandler) {
    this.connectionDeathHandlers = this.connectionDeathHandlers.filter((h) => h !== handler);
  }

  public removeAllConnectionDeathHandlers() {
    this.connectionDeathHandlers = [];
  }

  private async reconnectIfNecessary() {
    // the sending strategy will fail if we have no connection
    if (this.connections.length > 0) {
      console.info('Reconnecting to all disconnected websockets..');
      await Promise.all(this.connections.map((connection) => connection.reconnectIfNecessary()));
      console.info('Done. Execute re-connection handlers..');

      for (const handler of this.reconnectionHandlers) {
        handler();
      }
    }
  }

  private getConnectionByAddress(address: string): NetworkConnection | undefined {
    return this.connections.find((nc: NetworkConnection) => nc.address === address);
  }

  private onConnectionDeath(address: string) {
    this.disconnectFrom(address);
    this.connectionDeathHandlers.forEach((handler) => handler(address));
  }

  /** Connects to a server or returns an existing connection to the server
   * @param address the server's full address (URI)
   * @returns a new connection to the server, or an existing one if it's already established
   */
  public async connect(address: string): Promise<NetworkConnection> {
    if (!address) {
      throw new Error('No address provided in connect');
    }

    const { href } = new URL(address); // validate address

    const existingConnection = this.getConnectionByAddress(href);

    if (existingConnection !== undefined) {
      return existingConnection;
    }

    const connection: NetworkConnection = await NetworkConnection.create(
      href,
      this.rpcHandler,
      () => this.onConnectionDeath(href),
    );
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
   * @param connections - An optional list of network connection if the message should only be sent on a subset of connections
   *
   * @returns List of a promises being resolved with the responses,
   * or rejected with an error if the payload could not be delivered or was rejected by the server
   */
  public sendPayload(
    payload: JsonRpcRequest,
    connections?: NetworkConnection[],
  ): Promise<ExtendedJsonRpcResponse[]> {
    return this.sendingStrategy(payload, connections || this.connections);
  }

  public setRpcHandler(handler: JsonRpcHandler): void {
    this.rpcHandler = handler;
    this.connections.forEach((c) => c.setRpcHandler(handler));
  }
}

export function getNetworkManager(): NetworkManager {
  if (NETWORK_MANAGER_INSTANCE === undefined) {
    // TODO: decide what the desired strategy is
    NETWORK_MANAGER_INSTANCE = new NetworkManager(sendToAllServersStrategy);
  }
  return NETWORK_MANAGER_INSTANCE;
}

export const TEST_ONLY_EXPORTS = { NetworkManager };
