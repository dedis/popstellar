import { w3cwebsocket as W3CWebSocket } from 'websocket';
import { JsonRpcRequest, JsonRpcResponse, UNDEFINED_ID } from 'model/network';

const GENERATE_ID: number = -1;

const WEBSOCKET_READYSTATE_INTERVAL_MS = 10;
const WEBSOCKET_READYSTATE_MAX_ATTEMPTS = 100;

export class NetworkConnexion {
  private readonly ws: any;

  private payloadSentCount: number = 0;

  private payloadPending: Map<number, Promise<JsonRpcResponse>>;

  public readonly address: string; // FIXME get the address from ws (log ws when app is running)

  constructor(address: string, onMessageHandler: (message: any) => void) {
    const ws = new W3CWebSocket(address);

    ws.onopen = () => { console.info(`initiating web socket : ${address}`); };
    ws.onmessage = (message: any) => {
      console.log(`Received a new message from '${address}' : `, message.data);

      let response: JsonRpcResponse;
      try {
        response = JsonRpcResponse.fromJson(message.data);
      } catch (e) {
        throw new Error('Answer is not valid Json');
      }

      if (response.id !== UNDEFINED_ID) {
        const promise = this.payloadPending.get(response.id);
        this.payloadPending.delete(response.id);

        // FIXME resolve the promise
      }

      onMessageHandler(response);
    };
    ws.onerror = (event: any) => { console.error(`WebSocket error observed on '${address}' : `, event); };

    this.address = address;
    this.payloadPending = new Map();
    this.ws = ws;
  }

  public sendPayload(payload: JsonRpcRequest): void {
    // Check that the websocket connection is ready
    if (!this.ws.readyState) {
      new Promise((resolve, reject) => {
        this.waitWebsocketReady(resolve, reject);
      }).then(
        () => this.sendPayload(payload),
      );
    } else {
      let query: JsonRpcRequest = payload;

      // websocket ready to be used, message can be sent
      if (payload.id === undefined || payload.id === GENERATE_ID) {
        // Note: this only works because react/Js is single threaded
        query = new JsonRpcRequest({
          ...payload,
          id: this.payloadSentCount,
        });
        this.payloadSentCount += 1;
      }

      const promise = new Promise<JsonRpcResponse>(() => {
        // FIXME ?
        // how to resolve/reject the promise from outside?
        // Aka. when we receive an answer, we parse the id, take the
        // corresponding query out of the map and resolve the promise.
      });

      this.payloadPending.set(
        query.id as number, // as this point, there will be a number as id
        promise,
      );

      console.log(`Sending this message on '${this.address}' : `, query);
      this.ws.send(JSON.stringify(query));
    }
  }

  private waitWebsocketReady(resolve: any, reject: any) {
    if (!this.ws.readyState) {
      let count = 0;

      const id = window.setInterval(() => {
        if (!this.ws.readyState && count < WEBSOCKET_READYSTATE_MAX_ATTEMPTS) {
          count += 1;
        } else {
          // abandon if we reached too many attempts
          if (count === WEBSOCKET_READYSTATE_MAX_ATTEMPTS) {
            reject(
              `Maximum waiting time for websocket to be ready reached :
              ${WEBSOCKET_READYSTATE_MAX_ATTEMPTS * WEBSOCKET_READYSTATE_INTERVAL_MS}
              [ms] (_waitWebsocketReady)`,
            );
          } else resolve();
          window.clearInterval(id);
        }
      }, WEBSOCKET_READYSTATE_INTERVAL_MS);
    } else {
      resolve();
    }
  }
}
