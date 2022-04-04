import { describe, it } from '@jest/globals';

import { mockAddress, mockJsonResponse } from '__tests__/utils';
import { ProtocolError } from 'core/objects';

import { ExtendedJsonRpcResponse } from '../ExtendedJsonRpcResponse';
import { JsonRpcResponse } from '../JsonRpcResponse';

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
    expect(fn).toThrow(ProtocolError);
  });
});
