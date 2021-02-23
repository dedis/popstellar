import { w3cwebsocket as W3CWebSocket } from 'websocket';
import {
  JsonRpcRequest, JsonRpcResponse, ProtocolError, UNDEFINED_ID,
} from 'model/network';
import { NetworkError } from './NetworkError';

type ResponseHandler = (message: JsonRpcResponse) => void;

const DEFAULT_RESPONSE_HANDLER: ResponseHandler = (message: JsonRpcResponse) => {
  if (message.error === undefined) {
    // default handler does nothing which is useful for some messages
  } else {
    console.error(
      'A negative network error was received:\n'
      + `\t- error code : ${message.error.code}`
      + `\t- description : ${message.error.description}`,
    );
  }
};

const WEBSOCKET_READYSTATE_INTERVAL_MS = 10;
const WEBSOCKET_READYSTATE_MAX_ATTEMPTS = 100;

const WEBSOCKET_MESSAGE_TIMEOUT_MS = 10000; // 10 seconds max round-trip time

interface PendingResponse {
  promise: Promise<JsonRpcResponse>,
  resolvePromise: (value: JsonRpcResponse) => void,
  rejectPromise: () => void,
  timeoutId: NodeJS.Timeout,
}

export class NetworkConnection {
  private readonly ws: any;

  private payloadSentCount: number = 0;

  private payloadPending: Map<number, PendingResponse> = new Map();

  private onMessageHandler: ResponseHandler = DEFAULT_RESPONSE_HANDLER;

  public readonly address: string; // FIXME get the address from ws (log ws when app is running)

  constructor(address: string) {
    const ws = new W3CWebSocket(address);

    ws.onopen = () => { console.info(`Initiating web socket : ${address}`); };
    ws.onmessage = (message: any) => {
      console.log(`Received a new message from '${address}' : `, message.data);

      let response: JsonRpcResponse;
      try {
        response = JsonRpcResponse.fromJson(message.data);
      } catch (e) {
        throw new ProtocolError('Answer is not valid Json');
      }

      if (response.id !== UNDEFINED_ID) {
        const pendingResponse = this.payloadPending.get(response.id);
        if (pendingResponse === undefined) {
          throw new NetworkError(`Received a response which id = ${response.id} does not match any pending requests`);
        }

        this.payloadPending.delete(response.id);

        pendingResponse.resolvePromise(response);
        this.onMessageHandler(response);
      }
    };
    ws.onclose = () => { console.info(`Closed websocket connection : ${address}`); };
    ws.onerror = (event: any) => { console.error(`WebSocket error observed on '${address}' : `, event); };

    this.address = address;
    this.ws = ws;
  }

  public setResponseHandler(handler: ResponseHandler) {
    this.onMessageHandler = handler;
  }

  public sendPayload(payload: JsonRpcRequest): Promise<JsonRpcResponse> {
    // Check that the websocket connection is ready
    if (!this.ws.readyState) {
      return new Promise(() => {
        this.waitWebsocketReady().then(() => this.sendPayload(payload));
      });
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
          `Maximum waiting time of ${WEBSOCKET_MESSAGE_TIMEOUT_MS} ms reached, dropping query`,
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
      return new Promise((resolve) => { resolve(); });
    }

    let count = 0;
    return new Promise((resolve, reject) => {
      const id: NodeJS.Timeout = setInterval(() => {
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
