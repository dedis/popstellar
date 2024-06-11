import { generateUsernameFromBase64 } from '../Mnemonic';

test('generateUsernameFromBase64 should generate the correct username for the token: d_xeXEsurEnyWOp04mrrMxC3m4cS-3jK_9_Aw-UYfww=', () => {
  const popToken = 'd_xeXEsurEnyWOp04mrrMxC3m4cS-3jK_9_Aw-UYfww=';
  const result = 'spoonissue0434';
  expect(generateUsernameFromBase64(popToken)).toBe(result);
});

test('generateUsernameFromBase64 should generate the correct username for the token: e5YJ5Q6x39u_AIK78_puEE2X5wy7Y7iYZLeuZx1lnZI=', () => {
  const popToken = 'e5YJ5Q6x39u_AIK78_puEE2X5wy7Y7iYZLeuZx1lnZI=';
  const result = 'bidtrial5563';
  expect(generateUsernameFromBase64(popToken)).toBe(result);
});

test('generateUsernameFromBase64 should generate the correct username for the token: Y5ZAd_7Ba31uu4EUIYbG2AVnthR623-NdPyYhtyechE=', () => {
  const popToken = 'Y5ZAd_7Ba31uu4EUIYbG2AVnthR623-NdPyYhtyechE=';
  const result = 'figuredevote5731';
  expect(generateUsernameFromBase64(popToken)).toBe(result);
});
