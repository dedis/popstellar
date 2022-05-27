import { mockLaoId } from '__tests__/utils';

import { encodeLaoConnectionForQRCode } from '../LaoConnection';

describe('lao-connection', () => {
  describe('encodeLaoConnectionInQRCode', () => {
    it('should return correctly encoded data', () => {
      const servers = ['some server'];
      const data = JSON.parse(encodeLaoConnectionForQRCode(servers, mockLaoId));
      expect(data).toHaveProperty('servers', servers);
      expect(data).toHaveProperty('lao', mockLaoId);
    });
  });
});
