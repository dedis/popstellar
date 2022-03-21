import { mockLaoId } from '__tests__/utils';

import { encodeLaoConnectionInQRCode } from '../lao-connection';

describe('lao-connection', () => {
  describe('encodeLaoConnectionInQRCode', () => {
    it('should return correctly encoded data', () => {
      const server = 'some server';
      const data = JSON.parse(encodeLaoConnectionInQRCode(server, mockLaoId));
      expect(data).toHaveProperty('server', server);
      expect(data).toHaveProperty('lao', mockLaoId);
    });
  });
});
