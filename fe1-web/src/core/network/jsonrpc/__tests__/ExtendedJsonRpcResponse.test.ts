import { describe, it } from '@jest/globals';

import { ExtendedJsonRpcResponse } from '../ExtendedJsonRpcResponse';
import { JsonRpcResponse } from '../JsonRpcResponse';

const mockAddress = 'some address';
const mockJsonResponse: Partial<JsonRpcResponse> = { id: 0, result: [] };

describe('ExtendedJsonRpcResponse', () => {
  it('can create a new instance', () => {
    const extendedResponse = new ExtendedJsonRpcResponse(
      { receivedFrom: mockAddress },
      mockJsonResponse,
    );
    expect(extendedResponse.response).toEqual(new JsonRpcResponse(mockJsonResponse));
    expect(extendedResponse.receivedFrom).toEqual(mockAddress);
  });

  it('cannot create a new instance with missing receivedFrom parameter', () => {
    const fn = () => new ExtendedJsonRpcResponse({}, mockJsonResponse);
    expect(fn).toThrow();
  });
});
