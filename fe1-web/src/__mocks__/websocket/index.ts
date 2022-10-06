import { jest } from '@jest/globals';
import { ICloseEvent, IMessageEvent, w3cwebsocket as W3CWebSocket } from 'websocket';

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
    setTimeout(this.mockOnOpen.bind(this), 100);
  }

  public mockOnOpen() {
    this.onopen();
  }

  public mockOnMessage(message: IMessageEvent) {
    this.onmessage(message);
  }

  public mockOnClose(event: ICloseEvent) {
    this.onclose(event);
  }

  public mockOnError(error: Error) {
    this.onerror(error);
  }

  public close = jest.fn(() => {
    this.readyState = 3;
  });

  public send = jest.fn();
}

export type MockedW3CWebSocket = w3cwebsocket;
