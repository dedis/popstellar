import { describe, it } from '@jest/globals';

import { ExtendedJsonRpcRequest } from '../ExtendedJsonRpcRequest';
import { JsonRpcMethod } from '../JsonRpcMethods';
import { JsonRpcRequest } from '../JsonRpcRequest';

const mockAddress = 'some address';
const mockChannel = 'some channel';
const mockJsonRequest: Partial<JsonRpcRequest> = {
  jsonrpc: 'some data',
  method: JsonRpcMethod.BROADCAST,
  params: { channel: mockChannel },
};

describe('ExtendedJsonRpcRequest', () => {
  it('can create a new instance', () => {
    const extendedRequest = new ExtendedJsonRpcRequest(
      { receivedFrom: mockAddress },
      mockJsonRequest,
    );
    expect(extendedRequest.request).toEqual(new JsonRpcRequest(mockJsonRequest));
    expect(extendedRequest.receivedFrom).toEqual(mockAddress);
  });

  it('cannot create a new instance with missing receivedFrom parameter', () => {
    const fn = () => new ExtendedJsonRpcRequest({}, mockJsonRequest);
    expect(fn).toThrow();
  });
});
