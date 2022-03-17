import { jest } from '@jest/globals';

import { NetworkError } from '..';

export class MockNetworkConnection {
  public readonly address: string;

  public sendingSucceeds: boolean;

  public sendResponse: any;

  public sendError: string;

  constructor(address: string, sendingSucceeds: boolean, sendResponse?: any, sendError?: string) {
    this.address = address;
    this.sendingSucceeds = sendingSucceeds;
    this.sendResponse = sendResponse || 'response';
    this.sendError = sendError || 'error';
  }

  public disconnect = jest.fn();

  public setRpcHandler = jest.fn();

  public sendPayload = jest.fn().mockImplementation(() => {
    if (this.sendingSucceeds) {
      return Promise.resolve(this.sendResponse);
    }
    return Promise.reject(new Error(this.sendError));
  });
}
