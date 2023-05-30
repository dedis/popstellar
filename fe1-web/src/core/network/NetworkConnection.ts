import { IMessageEvent, w3cwebsocket as W3CWebSocket } from 'websocket';

import { ProtocolError } from 'core/objects';

import {
  ExtendedJsonRpcRequest,
  ExtendedJsonRpcResponse,
  JsonRpcRequest,
  JsonRpcResponse,
  UNDEFINED_ID,
} from './jsonrpc';
import { NetworkError } from './NetworkError';
import { defaultRpcHandler, JsonRpcHandler } from './RpcHandler';
import { RpcOperationError } from './RpcOperationError';

const WEBSOCKET_CONNECTION_MAX_ATTEMPTS = 5;
const WEBSOCKET_CONNECTION_FAILURE_TIMEOUT_MS = 500;

const WEBSOCKET_READYSTATE_INTERVAL_MS = 250;
const WEBSOCKET_READYSTATE_MAX_ATTEMPTS = 10;
const WEBSOCKET_MESSAGE_TIMEOUT_MS = 10000; // 10 seconds max round-trip time

const JSON_RPC_ID_WRAP_AROUND = 10000;

interface PendingRequest {
  resolvePromise: (value: ExtendedJsonRpcResponse) => void;
  rejectPromise: (error: NetworkError) => void;
  timeoutId: ReturnType<typeof setTimeout>;
  payload: JsonRpcRequest;
}

export class NetworkConnection {
  /**
   * The associated websocket connection
   */
  private ws: W3CWebSocket;

  /**
   * The next json rpc request id to be used
   */
  private nextRpcId: number = 1;

  /**
   * A map from rpc id to pending requests
   */
  private readonly pendingRequests: Map<number, PendingRequest> = new Map();

  /**
   * The handler used for handling json rpc requests
   */
  private onRpcHandler: JsonRpcHandler = defaultRpcHandler;

  /**
   * The address where to connect to
   */
  public readonly address: string;

  /**
   * The number of failed connection attempts after the connection broke
   * because of an error
   */
  private failedConnectionAttempts: number = 0;

  /**
   * Function called when the first websocket connection is established
   */
  private onInitialOpenCallback?: () => void;

  /**
   * The timeout set when initially opening a connection
   */
  private readonly initalOpenTimeout: ReturnType<typeof setTimeout>;

  /**
   * Function called when the connection closes for good
   */
  private readonly onConnectionDeathCallback?: () => void;

  /**
   * Boolean indicating whether this connection is open or there is still hope
   * in re-establishing a broken conneciton
   */
  private alive: boolean;

  /**
   * Boolean indicating whether we wanted to close it
   */
  private closeIntent: boolean;

  /**
   * Timeouts per instance. Only here so that
   * the tests can go brrr
   */
  private readonly websocketConnectionTimeout: number;

  private readonly websocketMessageTimeout: number;

  /**
   * SHOULD *NOT* BE CALLED DIRECTLY.
   * USE NetworkConnection.create() instead
   */
  constructor(
    address: string,
    handler?: JsonRpcHandler,
    onInitialOpenCallback?: () => void,
    onInitialOpenTimeout?: () => void,
    onConnectionDeathCallback?: () => void,
    websocketConnectionTimeout = WEBSOCKET_CONNECTION_FAILURE_TIMEOUT_MS,
    websocketMessageTimeout = WEBSOCKET_MESSAGE_TIMEOUT_MS,
  ) {
    this.address = address;
    this.onRpcHandler = handler !== undefined ? handler : defaultRpcHandler;
    this.onInitialOpenCallback = onInitialOpenCallback;
    this.onConnectionDeathCallback = onConnectionDeathCallback;
    this.alive = true;
    this.closeIntent = false;
    this.websocketConnectionTimeout = websocketConnectionTimeout;
    this.websocketMessageTimeout = websocketMessageTimeout;

    this.initalOpenTimeout = setTimeout(() => {
      // if the initial timeout expires, we deem this connection
      // dead. this signals the rest of the class to ignore
      // any further data that is received
      this.alive = false;

      // this.ws might not be set yet
      if (this.ws) {
        this.ws.close();
      }
      if (this.onConnectionDeathCallback) {
        this.onConnectionDeathCallback();
      }

      if (onInitialOpenTimeout) {
        onInitialOpenTimeout();
      }
    }, this.websocketConnectionTimeout);

    this.ws = this.establishConnection(address);
  }

  private establishConnection(address: string): W3CWebSocket {
    const ws: W3CWebSocket = new W3CWebSocket(address);

    ws.onopen = this.onOpen.bind(this);
    ws.onmessage = this.onMessage.bind(this);
    ws.onclose = this.onClose.bind(this);
    ws.onerror = this.onError.bind(this);

    return ws;
  }

  /**
   * Creates a new NetworkConnection instance
   * @param address The address to connect to
   * @param handler The json rpc message handler
   * @param onConnectionDeathCallback A function called when the connection closes for good because of an error
   * @param websocketConnectionTimeout The timeout for new websocket connections
   * @param websocketMessageTimeout The timeout for individual messages
   * @returns A promise with the network connection if a connection can be established
   */
  static create(
    address: string,
    handler: JsonRpcHandler,
    onConnectionDeathCallback: () => void,
    websocketConnectionTimeout?: number,
    websocketMessageTimeout?: number,
  ): Promise<[NetworkConnection, NetworkError | null]> {
    return new Promise((resolve) => {
      const nc: NetworkConnection = new NetworkConnection(
        address,
        handler,
        () => resolve([nc, null]),
        () => resolve([nc, new NetworkError(`Connecting to ${address} timed out.`)]),
        onConnectionDeathCallback,
        websocketConnectionTimeout,
        websocketMessageTimeout,
      );
    });
  }

  /**
   * Closes the websocket connection
   */
  public disconnect(): void {
    this.closeIntent = true;
    this.ws.close();
  }

  /**
   * If the websocket is in the closing state or has already been closed,
   * this function tries to re-establish the connection
   */
  public reconnectIfNecessary(): Promise<void> {
    // 0 = CONNECTING, 1 = OPEN, 2 = CLOSING, 3 = CLOSED
    if (this.ws.readyState > 1) {
      return new Promise((resolve, reject) => {
        const connectionTimeout = setTimeout(
          () => reject(new NetworkError(`Re-connecting to ${this.address} timed out.`)),
          this.websocketConnectionTimeout,
        );

        this.onInitialOpenCallback = () => {
          resolve();
          clearTimeout(connectionTimeout);
        };

        // reset state
        this.alive = true;
        this.closeIntent = false;
        this.ws = this.establishConnection(this.address);
      });
    }

    return Promise.resolve();
  }

  /**
   * Function called when the websocket connection opens / is established
   */
  private onOpen(): void {
    // only execute function if the connection is still alive
    // (it could be that due to a timeout this connection is already deemed dead)
    if (!this.alive) {
      return;
    }

    console.info(`Initiating web socket : ${this.address}`);
    clearTimeout(this.initalOpenTimeout);

    // reset failed connection attempts
    this.failedConnectionAttempts = 0;

    // when the connection is established for the first time
    // call onInitialOpenCallback exactly once
    if (this.onInitialOpenCallback) {
      this.onInitialOpenCallback();
      this.onInitialOpenCallback = undefined;
    }

    // if there are pending requests, re-send them
    // this can happen after the connection was re-established
    for (const entry of this.pendingRequests.entries()) {
      const request: PendingRequest = entry[1];

      // clear timeout, connection was just re-established
      clearTimeout(request.timeoutId);
      this.resendRequest(request);
    }
  }

  /**
   * Function called when a message over the websocket connection is received
   * @param message The websocket message
   */
  private onMessage(message: IMessageEvent): void {
    console.debug(`Received a new message from '${this.address}' : `, message.data);

    try {
      const parsedMessage: JsonRpcResponse | JsonRpcRequest = NetworkConnection.parseIncomingData(
        message.data,
      );

      if (parsedMessage instanceof JsonRpcResponse) {
        this.processResponse(parsedMessage);
      } /* instanceof JsonRpcRequest */ else {
        this.processRequest(parsedMessage);
      }
    } catch (e) {
      console.warn(`Received invalid message:\n${message.data}\n\n`, e);
    }
  }

  /**
   * Function called when the connection has been closed, either on purpose or because of an error
   */
  private onClose(): void {
    // only execute function if the connection is still alive
    // (it could be that due to a timeout this connection is already deemed dead)
    if (!this.alive) {
      return;
    }

    console.info(`Closed websocket connection : ${this.address}`);

    // check if we actually wanted to close this connection
    if (this.closeIntent) {
      this.alive = false;
      return;
    }

    // it was not our intention to close the connection. try to re-connect
    this.failedConnectionAttempts += 1;
    console.error(`Trying to re-establish a connection at address : ${this.address}`);

    // only retry a certain number of times and add a wait before retrying
    if (this.failedConnectionAttempts <= WEBSOCKET_CONNECTION_MAX_ATTEMPTS) {
      setTimeout(() => {
        this.reconnectIfNecessary().catch(()=>{});
      }, this.websocketConnectionTimeout);
      return;
    }

    console.error(`Connection with ${this.address} broke for good`);
    this.alive = false;

    if (this.onConnectionDeathCallback) {
      // do not try to re-establish the connection, this connection seems
      // to be broken for now and the near future
      this.onConnectionDeathCallback();
    }
  }

  /**
   * Function called when the websocket connection fails because of an error
   */
  private onError(event: Error): void {
    // only execute function if the connection is still alive
    // (it could be that due to a timeout this connection is already deemed dead)
    if (!this.alive) {
      return;
    }

    console.error(`WebSocket error observed on '${this.address}' : `, event);
  }

  /**
   * Parses the incoming data
   * @param data The data received over the websocket
   * @returns A parsed json rpc response or request
   */
  private static parseIncomingData(data: any): JsonRpcResponse | JsonRpcRequest {
    let errResp: any;
    let errReq: any;

    try {
      return JsonRpcResponse.fromJson(data);
    } catch (e) {
      errResp = e;
    }

    try {
      return JsonRpcRequest.fromJson(data);
    } catch (e) {
      errReq = e;
    }

    throw new ProtocolError(
      'Failed to parse incoming data as valid JSON based on protocol.' +
        ` Errors:\n\n${errResp}\n\n${errReq}`,
    );
  }

  /**
   * Forwards a received json rpc request to the rpc handler
   */
  private processRequest(request: JsonRpcRequest): void {
    try {
      this.onRpcHandler(new ExtendedJsonRpcRequest({ receivedFrom: this.address }, request));
    } catch (e) {
      console.error('Exception encountered when processing request:', request, e);
    }
  }

  /**
   * Finds the pending request for a received response
   * and resolve the respective promise
   */
  private processResponse(parsedMessage: JsonRpcResponse): void {
    if (parsedMessage.id !== UNDEFINED_ID) {
      const pendingResponse = this.pendingRequests.get(parsedMessage.id);
      if (pendingResponse === undefined) {
        throw new NetworkError(
          `Received a response whose id = ${parsedMessage.id}` +
            ' does not match any pending requests',
        );
      }

      // a response was received, clear the timeout and the pending query from the pending map
      clearTimeout(pendingResponse.timeoutId);
      this.pendingRequests.delete(parsedMessage.id);

      // use promise resolve/reject to communicate RPC outcome
      if (parsedMessage.error === undefined) {
        pendingResponse.resolvePromise(
          new ExtendedJsonRpcResponse({ receivedFrom: this.address }, parsedMessage),
        );
      } else {
        pendingResponse.rejectPromise(
          new RpcOperationError(
            parsedMessage.error.description,
            parsedMessage.error.code,
            parsedMessage.error.data,
          ),
        );
      }
    }
  }

  /**
   * Updates the rpc handler for received json rpc requests
   */
  public setRpcHandler(handler: JsonRpcHandler): void {
    this.onRpcHandler = handler;
  }

  /**
   * Tries to send a payload and waits for its response
   * @param payload The payload to send
   * @returns A promise that resolves when the payload was sent sucessfully and a response was received
   */
  public async sendPayload(payload: JsonRpcRequest): Promise<ExtendedJsonRpcResponse> {
    // Check that the websocket connection is ready
    if (this.ws.readyState !== 1 /* CONNECTING | CLOSING | CLOSED */) {
      await this.waitWebsocketReady();
      return this.sendPayload(payload);
    }

    // websocket ready to be used, message can be sent
    // Note: this only works because react/Js is single threaded
    const query: JsonRpcRequest = new JsonRpcRequest({
      ...payload,
      id: this.getNextRpcID(),
    });

    return new Promise<ExtendedJsonRpcResponse>((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        this.pendingRequests.delete(query.id as number);
        reject(
          new NetworkError(
            `Maximum waiting time of ${this.websocketMessageTimeout} [ms] reached, dropping query`,
          ),
        );
      }, this.websocketMessageTimeout);

      const pendingRequest: PendingRequest = {
        resolvePromise: resolve,
        rejectPromise: reject,
        timeoutId: timeoutId,
        payload: query,
      };

      this.pendingRequests.set(
        query.id as number, // as this point, there will be a number as id
        pendingRequest,
      );

      console.log(`Sending this message on '${this.address}' : `, query);
      this.ws.send(JSON.stringify(query));
    });
  }

  /**
   * Tries to re-send a pending request
   * @param request The request to resend
   */
  private async resendRequest(request: PendingRequest): Promise<void> {
    // Check that the websocket connection is ready
    if (this.ws.readyState !== 1 /* CONNECTING | CLOSING | CLOSED */) {
      await this.waitWebsocketReady();
      this.resendRequest(request);

      return;
    }

    const resolve = request.resolvePromise;
    const reject = request.rejectPromise;

    const timeoutId = setTimeout(() => {
      this.pendingRequests.delete(request.payload.id as number);
      reject(
        new NetworkError(
          `Maximum waiting time of ${this.websocketMessageTimeout} [ms] reached, dropping query`,
        ),
      );
    }, this.websocketMessageTimeout);

    const newPendingRequest: PendingRequest = {
      resolvePromise: resolve,
      rejectPromise: reject,
      timeoutId: timeoutId,
      payload: request.payload,
    };

    // override map entry
    this.pendingRequests.set(
      request.payload.id as number, // as this point, there will be a number as id
      newPendingRequest,
    );

    console.log(`Re-sending this message on '${this.address}' : `, request.payload);
    this.ws.send(JSON.stringify(request.payload));
  }

  /**
   * Generates consecutive json rpc ids
   */
  private getNextRpcID(): number {
    /* This function should also make sure that IDs used by the connection peer are not reused.
     * In practice, not doing this check is okay as the server doesn't initiate RPCs
     */
    const rpcId = this.nextRpcId;
    this.nextRpcId = (this.nextRpcId + 1) % JSON_RPC_ID_WRAP_AROUND;
    return rpcId;
  }

  /**
   * Allows to wait for the websocket readyState to switch to OPEN within a fixed amount of retries
   * @returns The promise
   */
  private waitWebsocketReady(): Promise<void> {
    if (this.ws.readyState === 1 /* OPEN */) {
      return Promise.resolve();
    }

    // the connection broke before we could send our message
    // and onError should be trying to restore it
    // thus wait for a little bit and re-check from
    // time to time

    let count = 0;
    return new Promise((resolve, reject) => {
      const id = setInterval(() => {
        if (this.ws.readyState === 1 /* OPEN */ && this.alive) {
          clearInterval(id);
          resolve();
        } else if (
          this.ws.readyState !== 1 /* CONNECTING | CLOSING | CLOSED */ &&
          count < WEBSOCKET_READYSTATE_MAX_ATTEMPTS &&
          this.alive
        ) {
          count += 1;
        } else if (this.alive) {
          // abandon if we reached too many attempts
          reject(
            new NetworkError(
              `Maximum waiting time for websocket to be ready reached : ${
                WEBSOCKET_READYSTATE_MAX_ATTEMPTS * WEBSOCKET_READYSTATE_INTERVAL_MS
              } [ms] (waitWebsocketReady)`,
            ),
          );
        } else {
          // the connection broke for good
          reject(
            new NetworkError(
              `Connection broke for good and cannot re-established in the near future`,
            ),
          );
        }
      }, WEBSOCKET_READYSTATE_INTERVAL_MS);
    });
  }
}
