import { mockLaoId } from '__tests__/utils';

import { encodeLaoConnectionForQRCode } from '../LaoConnection';

describe('LaoConnection', () => {
  describe('encodeLaoConnectionInQRCode', () => {
    it('should return correctly encoded data', () => {
      const servers = ['some server'];
      const data = JSON.parse(encodeLaoConnectionForQRCode(servers, mockLaoId));
      expect(data).toHaveProperty('server', servers[0]);
      expect(data).toHaveProperty('lao', mockLaoId.toState());
    });
  });
});
