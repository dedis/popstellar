import { w3cwebsocket as W3CWebSocket } from 'websocket';
// eslint-disable-next-line import/no-cycle
import handleServerAnswer from './WebsocketAnswer';
import { PendingRequest } from './WebsocketUtils';

/* global window */ // do not touch! EsLint required comment!
/* eslint-disable no-underscore-dangle */

const WEBSOCKET_READYSTATE_INTERVAL_MS = 10;
const WEBSOCKET_READYSTATE_MAX_ATTEMPTS = 100;

/* TEMP */
const SERVER_ADDRESS = {
  address: '127.0.0.1',
  port: '8000', // '8080',
  path: '', // 'ps'
};

export default class WebsocketLink {
  // websocket connexion
  static #ws;

  // map of pending queries sent to server
  static #pendingQueries;

  // number of queries sent (used for id generation)
  static #queriesSentCount;

  /**
   * Send a request (query) to the server
   *
   * @param message (JsonObject) message for the server
   * @param requestObject (objects enum) object of the message
   * @param requestAction (actions enum) action of the message
   * @param retry (boolean) is the query a retry?
   */
  static sendRequestToServer(message, requestObject, requestAction, retry = false) {
    if (this.#ws == null) {
      WebsocketLink._initWebsocket(
        SERVER_ADDRESS.address, SERVER_ADDRESS.port, SERVER_ADDRESS.path,
      );
    }

    WebsocketLink._sendMessage(message, requestObject, requestAction, retry);
  }

  /** Return a map of (id -> PendingRequest) which are pending requests sent to the server */
  static getPendingProperties() { return this.#pendingQueries; }

  /**
   * Initialize a websocket connexion
   *
   * @param address address of the server
   * @param port port the server is listen to
   * @param path (optional) path the server is listen to
   * @private private method
   */
  static _initWebsocket(address = '127.0.0.1', port = '8000', path = '') {
    let p = path;
    if (path !== '') { p = `/${path}`; }
    const ws = new W3CWebSocket(`ws://${address}:${port}${p}`);

    ws.onopen = () => { console.log(`initiating web socket : ws://${address}:${port}`); };
    ws.onmessage = (message) => {
      try {
        handleServerAnswer(message);
      } catch (e) {
        console.error(`Exception in handleServerAnswer: ${e.message}`);
      }
    };
    // TODO on error

    this.#ws = ws;
    this.#queriesSentCount = 0;
    this.#pendingQueries = new Map();
  }

  /**
   * Signal when an open websocket connexion is ready to be used
   *
   * @param resolveWebsocketReady positive executor of the caller's promise
   * @param rejectWebsocketReady negative executor of the caller's promise
   * @private private method
   */
  static _waitWebsocketReady(resolveWebsocketReady, rejectWebsocketReady) {
    if (!this.#ws.readyState) {
      let count = 0;

      const id = window.setInterval(() => {
        if (!this.#ws.readyState && count < WEBSOCKET_READYSTATE_MAX_ATTEMPTS) {
          count += 1;
        } else {
          // abandon if we reached too many attempts
          if (count === WEBSOCKET_READYSTATE_MAX_ATTEMPTS) {
            rejectWebsocketReady(
              `Maximum waiting time for websocket to be ready reached : 
              ${WEBSOCKET_READYSTATE_MAX_ATTEMPTS * WEBSOCKET_READYSTATE_INTERVAL_MS}
              [ms] (_waitWebsocketReady)`,
            );
          } else resolveWebsocketReady();
          window.clearInterval(id);
        }
      }, WEBSOCKET_READYSTATE_INTERVAL_MS);
    } else {
      resolveWebsocketReady();
    }
  }

  /**
   * Effectively send a message to the server
   *
   * @param message (JsonObject) message for the server
   * @param requestObject (objects enum) object of the message
   * @param requestAction (actions enum) action of the message
   * @param retry (boolean) is the query a retry?
   * @private private method
   */
  static _sendMessage(message, requestObject, requestAction, retry) {
    // Check that the websocket connection is ready
    if (!this.#ws.readyState) {
      const promise = new Promise((resolveWebsocketReady, rejectWebsocketReady) => {
        this._waitWebsocketReady(resolveWebsocketReady, rejectWebsocketReady);
      });

      promise.then(
        () => this._sendMessage(message, requestObject, requestAction, retry),
        (error) => console.error('(TODO)', error),
      );
    } else {
      // websocket ready to be used, message can be sent
      const query = JSON.parse(JSON.stringify(message)); // defensive copy
      if (query.id === -1) {
        // Note: this works because react/Js is single threaded
        query.id = this.#queriesSentCount;
        this.#queriesSentCount += 1;
      }

      if (!retry) {
        this.#pendingQueries.set(
          query.id,
          new PendingRequest(query, requestObject, requestAction),
        );
      }
      this.#ws.send(JSON.stringify(query));
    }
  }
}
