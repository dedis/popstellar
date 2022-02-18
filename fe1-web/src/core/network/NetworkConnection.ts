import { w3cwebsocket as W3CWebSocket } from 'websocket';
import { ProtocolError } from 'core/objects';
import { JsonRpcRequest, JsonRpcResponse, UNDEFINED_ID } from 'core/network/jsonrpc';
import { RpcOperationError } from 'core/network/jsonrpc/RpcOperationError';
import { NetworkError } from 'core/network/NetworkError';
import { defaultRpcHandler, JsonRpcHandler } from 'core/network/jsonrpc/RpcHandler';

const WEBSOCKET_READYSTATE_INTERVAL_MS = 10;
const WEBSOCKET_READYSTATE_MAX_ATTEMPTS = 100;

const WEBSOCKET_MESSAGE_TIMEOUT_MS = 10000; // 10 seconds max round-trip time

const JSON_RPC_ID_WRAP_AROUND = 10000;

interface PendingResponse {
  promise: Promise<JsonRpcResponse>;
  resolvePromise: (value: JsonRpcResponse) => void;
  rejectPromise: (error: RpcOperationError) => void;
  timeoutId: ReturnType<typeof setTimeout>;
}

export class NetworkConnection {
  private ws: W3CWebSocket;

  private nextRpcId: number = 1;

  private readonly payloadPending: Map<number, PendingResponse> = new Map();

  private onRpcHandler: JsonRpcHandler = defaultRpcHandler;

  public readonly address: string;

  constructor(address: string, handler?: JsonRpcHandler) {
    this.ws = this.establishConnection(address);
    this.address = this.ws.url;
    this.onRpcHandler = handler !== undefined ? handler : defaultRpcHandler;
  }

  private establishConnection(address: string): W3CWebSocket {
    const ws: W3CWebSocket = new W3CWebSocket(address);

    ws.onopen = () => this.onOpen();
    ws.onmessage = (message: any) => this.onMessage(message);
    ws.onclose = () => this.onClose();
    ws.onerror = (event: any) => this.onError(event);

    return ws;
  }

  public disconnect(): void {
    this.ws.close();
  }

  private onOpen(): void {
    console.info(`Initiating web socket : ${this.address}`);
  }

  private onMessage(message: any): void {
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

  private onClose(): void {
    console.info(`Closed websocket connection : ${this.address}`);
  }

  private onError(event: any): void {
    console.error(`WebSocket error observed on '${this.address}' : `, event);
    console.error(`Trying to establish a new connection at address : ${this.address}`);
    this.ws = this.establishConnection(this.address);
  }

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

  private processRequest(request: JsonRpcRequest): void {
    try {
      this.onRpcHandler(request);
    } catch (e) {
      console.error('Exception encountered when processing request:', request, e);
    }
  }

  private processResponse(parsedMessage: JsonRpcResponse): void {
    if (parsedMessage.id !== UNDEFINED_ID) {
      const pendingResponse = this.payloadPending.get(parsedMessage.id);
      if (pendingResponse === undefined) {
        throw new NetworkError(
          `Received a response whose id = ${parsedMessage.id}` +
            ' does not match any pending requests',
        );
      }

      // a response was received, clear the timeout and the pending query from the pending map
      clearTimeout(pendingResponse.timeoutId);
      this.payloadPending.delete(parsedMessage.id);

      // use promise resolve/reject to communicate RPC outcome
      if (parsedMessage.error === undefined) {
        pendingResponse.resolvePromise(parsedMessage);
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

  public setRpcHandler(handler: JsonRpcHandler): void {
    this.onRpcHandler = handler;
  }

  public sendPayload(payload: JsonRpcRequest): Promise<JsonRpcResponse> {
    // Check that the websocket connection is ready
    if (!this.ws.readyState) {
      return this.waitWebsocketReady().then(() => this.sendPayload(payload));
    }

    // websocket ready to be used, messages can be sent
    let query: JsonRpcRequest = payload;

    // Note: this only works because react/Js is single threaded
    query = new JsonRpcRequest({
      ...payload,
      id: this.getNextRpcID(),
    });

    const promise = new Promise<JsonRpcResponse>((resolve, reject) => {
      const timeoutId = setTimeout(() => {
        this.payloadPending.delete(query.id as number);
        reject(
          new NetworkError(
            `Maximum waiting time of ${WEBSOCKET_MESSAGE_TIMEOUT_MS} [ms] reached, dropping query`,
          ),
        );
      }, WEBSOCKET_MESSAGE_TIMEOUT_MS);

      const pendingResponse: PendingResponse = {
        promise: promise,
        resolvePromise: resolve,
        rejectPromise: reject,
        timeoutId: timeoutId,
      };

      this.payloadPending.set(
        query.id as number, // as this point, there will be a number as id
        pendingResponse,
      );

      console.log(`Sending this message on '${this.address}' : `, query);
      this.ws.send(JSON.stringify(query));
    });

    return promise;
  }

  private getNextRpcID(): number {
    /* This function should also make sure that IDs used by the connection peer are not reused.
     * In practice, not doing this check is okay as the server doesn't initiate RPCs
     */
    const rpcId = this.nextRpcId;
    this.nextRpcId = (this.nextRpcId + 1) % JSON_RPC_ID_WRAP_AROUND;
    return rpcId;
  }

  private waitWebsocketReady(): Promise<void> {
    if (this.ws.readyState) {
      return Promise.resolve();
    }

    let count = 0;
    return new Promise((resolve, reject) => {
      const id: number = setInterval(() => {
        if (this.ws.readyState) {
          clearInterval(id);
          resolve();
        } else if (count < WEBSOCKET_READYSTATE_MAX_ATTEMPTS) {
          count += 1;
        } else {
          // abandon if we reached too many attempts
          clearInterval(id);
          reject(
            new NetworkError(
              `Maximum waiting time for websocket to be ready reached :
            ${WEBSOCKET_READYSTATE_MAX_ATTEMPTS * WEBSOCKET_READYSTATE_INTERVAL_MS}
            [ms] (_waitWebsocketReady)`,
            ),
          );
        }
      }, WEBSOCKET_READYSTATE_INTERVAL_MS);
    });
  }
}
