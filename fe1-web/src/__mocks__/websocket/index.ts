import { jest } from '@jest/globals';
import { w3cwebsocket as W3CWebSocket } from 'websocket';

type NetworkMessage = string;
type NetworkMessageHandler = (ws: w3cwebsocket, message: NetworkMessage) => void;

let networkMessageHandlers: NetworkMessageHandler[] = [];

export const addNetworkMessageHandler = (handler: NetworkMessageHandler) =>
  networkMessageHandlers.push(handler);

export const removeNetworkMessageHandler = (handler: NetworkMessageHandler) => {
  networkMessageHandlers = networkMessageHandlers.filter((h) => h !== handler);
};

export const clearNetworkMessageHandlers = () => {
  networkMessageHandlers = [];
};

let areNewConnectionsPossible = true;

export const allowNewConnections = () => {
  areNewConnectionsPossible = true;
};

export const disallowNewConnections = () => {
  areNewConnectionsPossible = false;
};

// the name of the mock must match exactly
// eslint-disable-next-line @typescript-eslint/naming-convention
export class w3cwebsocket {
  private readonly url: string;

  public readyState = 0; // 0 = CONNECTING, 1 = OPEN, 2 = CLOSING, 3 = CLOSED

  public onopen: W3CWebSocket['onopen'] = jest.fn();

  public onmessage: W3CWebSocket['onmessage'] = jest.fn();

  public onclose: W3CWebSocket['onclose'] = jest.fn();

  public onerror: W3CWebSocket['onerror'] = jest.fn();

  constructor(url: string) {
    this.url = url;

    // mock connection establishment
    setTimeout(() => {
      if (areNewConnectionsPossible) {
        this.mockOnOpen();
      } else {
        this.mockConnectionError(new Error('Could not reach destination'));
      }
    }, 100);
  }

  public mockOnOpen() {
    this.readyState = 1;
    this.onopen();
  }

  public mockReceive(message: string) {
    this.onmessage({ data: message });
  }

  public mockConnectionClose(wasClean: boolean, code?: number, reason?: string) {
    this.readyState = 3;
    this.onclose({ code: code || 0, reason: reason || 'No reason provided', wasClean });
  }

  public mockConnectionError(error: Error) {
    this.readyState = 3;
    this.onerror(error);
    this.onclose({ code: -1, reason: error.message, wasClean: false });
  }

  public close: W3CWebSocket['close'] = jest.fn((code?: number, reason?: string) => {
    this.readyState = 2;

    setTimeout(() => {
      this.readyState = 3;

      // https://developer.mozilla.org/en-US/docs/Web/API/WebSocket/close_event
      this.onclose({
        /* A unsigned short containing the close code sent by the server. */
        code: code || 0,
        reason: reason || 'Client closed the connection',
        wasClean: true,
      });
    }, 100);
  });

  public send: W3CWebSocket['send'] = jest.fn((message: NetworkMessage) => {
    networkMessageHandlers.forEach((h) => h(this, message));
  });
}

export type MockedW3CWebSocket = w3cwebsocket;
