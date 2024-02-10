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

  private onNetworkChange(state: NetInfoState): Promise<void> {
    const isOnline = state.isConnected || false;
    if (!this.isOnline && isOnline) {
      // if we were disconnected before and are now reconnected to the network
      // then try to reconnect all connections
      return this.reconnectIfNecessary();
    }

    this.isOnline = isOnline;
    return Promise.resolve();
  }

  private onAppStateChange(status: AppStateStatus): Promise<void> {
    const isFocused = status === 'active';
    if (!this.isFocused && isFocused) {
      // if we were backgrounded and become active again, the websocket
      // connections are very likely to be broken
      return this.reconnectIfNecessary();
    }

    this.isFocused = isFocused;
    return Promise.resolve();
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
    this.disconnectFrom(address, false);
    this.connectionDeathHandlers.forEach((handler) => handler(address));
  }

  /** Connects to a server or returns an existing connection to the server
   * @param address the server's full address (URI)
   * @param alwaysPersistConnection Whether to persist the connection even if the initial connection attempt fails
   * @returns a new connection to the server, or an existing one if it's already established
   */
  public async connect(
    address: string,
    alwaysPersistConnection = true,
  ): Promise<NetworkConnection> {
    if (!address) {
      throw new Error('No address provided in connect');
    }

    const { href } = new URL(address); // validate address

    const existingConnection = this.getConnectionByAddress(href);

    if (existingConnection !== undefined) {
      return existingConnection;
    }

    const [connection, error] = await NetworkConnection.create(href, this.rpcHandler, () =>
      this.onConnectionDeath(href),
    );

    if (alwaysPersistConnection) {
      // always push the connection to the array so that we can re-connect later
      this.connections.push(connection);
    }

    // if the inital connection attempt failed, reject the promise
    if (error != null) {
      throw error;
    }

    // if it succeeded, return it
    return connection;
  }

  public disconnect(connection: NetworkConnection, intentional = true): void {
    const index = this.connections.indexOf(connection);
    if (index !== -1) {
      this.connections[index].disconnect();

      if (intentional) {
        this.connections.splice(index, 1);
      }
    }
  }

  public disconnectFrom(address: string, intentional = true): void {
    if (!address) {
      throw new Error('No address provided in disconnectFrom');
    }
    const connection = this.getConnectionByAddress(address);
    if (connection !== undefined) {
      this.disconnect(connection, intentional);
    }
  }

  public disconnectFromAll(intentional = true): void {
    this.connections.forEach((nc: NetworkConnection) => nc.disconnect());

    if (intentional) {
      this.connections = [];
    }
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
