import { describe, it } from '@jest/globals';

import { mockAddress, mockJsonRequest } from '__tests__/utils';
import { ProtocolError } from 'core/objects';

import { ExtendedJsonRpcRequest } from '../ExtendedJsonRpcRequest';
import { JsonRpcRequest } from '../JsonRpcRequest';

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
    expect(fn).toThrow(ProtocolError);
  });
});
