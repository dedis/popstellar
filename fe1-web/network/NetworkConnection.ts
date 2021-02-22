import { w3cwebsocket as W3CWebSocket } from 'websocket';
import {
  JsonRpcRequest, JsonRpcResponse, ProtocolError, UNDEFINED_ID,
} from 'model/network';
import { Broadcast } from 'model/network/method';
import { OperationError } from './OperationError';
import { NetworkError } from './NetworkError';

type ResponseHandler = (message: JsonRpcResponse) => void;

const DEFAULT_RESPONSE_HANDLER: ResponseHandler = (message: JsonRpcResponse) => {
  console.log('No response handler was provided to manage message : ', message);
};

const WEBSOCKET_READYSTATE_INTERVAL_MS = 10;
const WEBSOCKET_READYSTATE_MAX_ATTEMPTS = 100;

const WEBSOCKET_MESSAGE_TIMEOUT_MS = 1000; // 10 seconds max round-trip time

interface PendingResponse {
  promise: Promise<JsonRpcResponse>,
  resolvePromise: (value: JsonRpcResponse) => void,
  rejectPromise: (error: OperationError) => void,
  timeoutId: number,
}

export class NetworkConnection {
  private ws: W3CWebSocket;

  private payloadSentCount: number = 0;

  private payloadPending: Map<number, PendingResponse> = new Map();

  private onMessageHandler: ResponseHandler = DEFAULT_RESPONSE_HANDLER;

  public readonly address: string;

  constructor(address: string) {
    this.ws = this.establishConnection(address);
    this.address = this.ws.url;
  }

  private establishConnection(address: string): W3CWebSocket {
    const ws: W3CWebSocket = new W3CWebSocket(address);

    ws.onopen = () => this.onOpen();
    ws.onmessage = (message: any) => this.onMessage(message);
    ws.onclose = () => this.onClose();
    ws.onerror = (event: any) => this.onError(event);

    return ws;
  }

  private onOpen(): void {
    console.info(`Initiating web socket : ${this.address}`);
  }

  private onMessage(message: any): void {
    console.log(`Received a new message from '${this.address}' : `, message.data);

    try {
      const parsedMessage: JsonRpcResponse | JsonRpcRequest = NetworkConnection.parseIncomingData(
        message.data,
      );
      if (parsedMessage instanceof JsonRpcResponse) {
        // if we have a response
        this.processResponse(parsedMessage);
      } else {
        // if we have a request which needs to be forwarded
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
    let parsedMessage: JsonRpcResponse | JsonRpcRequest;
    try {
      parsedMessage = JsonRpcResponse.fromJson(data);
    } catch (e) {
      try {
        parsedMessage = JsonRpcRequest.fromJson(data);
      } catch (ee) {
        throw new ProtocolError(`Answer is not valid Json : ${e} && ${ee}`);
      }
    }

    return parsedMessage;
  }

  // eslint-disable-next-line class-methods-use-this
  private processRequest(parsedMessage: JsonRpcRequest): void {
    // Note to students : here should be the logic regarding witness message forwarding
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const broadcast: Broadcast = new Broadcast(parsedMessage.params);

    // ...
  }

  private processResponse(parsedMessage: JsonRpcResponse): void {
    if (parsedMessage.id !== UNDEFINED_ID) {
      const pendingResponse = this.payloadPending.get(parsedMessage.id);
      if (pendingResponse === undefined) {
        throw new NetworkError(`Received a response which id = ${parsedMessage.id} does not match any pending requests`);
      }

      // a response was received, clear the timeout and the pending query from the pending map
      clearTimeout(pendingResponse.timeoutId);
      this.payloadPending.delete(parsedMessage.id);

      // reject the promise if the response is negative
      if (parsedMessage.isPositiveResponse()) {
        pendingResponse.resolvePromise(parsedMessage);
        try {
          this.onMessageHandler(parsedMessage);
        } catch (e) {
          console.error('Exception encountered when giving response : ', parsedMessage, ' to its handler.\n', e);
        }
      } else {
        // Note : impossible to have an undefined error from now on due to isPositiveResponse()
        pendingResponse.rejectPromise(
          // @ts-ignore
          new OperationError(parsedMessage.error.description, parsedMessage.error.code),
        );
      }
    }
  }

  public setResponseHandler(handler: ResponseHandler): void {
    this.onMessageHandler = handler;
  }

  public sendPayload(payload: JsonRpcRequest): Promise<JsonRpcResponse> {
    // Check that the websocket connection is ready
    if (!this.ws.readyState) {
      return this.waitWebsocketReady().then(() => this.sendPayload(payload));
    }

    // websocket ready to be used, message can be sent
    let query: JsonRpcRequest = payload;

    // Note: this only works because react/Js is single threaded
    query = new JsonRpcRequest({
      ...payload,
      id: this.payloadSentCount,
    });
    this.payloadSentCount += 1;

    const promise = new Promise<JsonRpcResponse>((resolve, reject) => {
      const timeoutId = setTimeout(
        () => reject(new NetworkError(
          `Maximum waiting time of ${WEBSOCKET_MESSAGE_TIMEOUT_MS} [ms] reached, dropping query`,
        )), // FIXME if reject, delete from queue
        WEBSOCKET_MESSAGE_TIMEOUT_MS,
      );

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

  public disconnect(): void {
    this.ws.close();
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
          reject(new NetworkError(
            `Maximum waiting time for websocket to be ready reached :
            ${WEBSOCKET_READYSTATE_MAX_ATTEMPTS * WEBSOCKET_READYSTATE_INTERVAL_MS}
            [ms] (_waitWebsocketReady)`,
          ));
        }
      }, WEBSOCKET_READYSTATE_INTERVAL_MS);
    });
  }
}
