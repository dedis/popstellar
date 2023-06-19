import { Hash, PopToken, ProtocolError } from 'core/objects';

import { sendPopchaAuthRequest } from '../network/PopchaMessageApi';

/**
 * Verify the scanned info (url)
 * @param data the scanned data
 * @param laoId the lao id
 * @returns an error message if the scanned info is invalid, undefined otherwise
 */
const verifyScannedInfo = (data: string, laoId: Hash) => {
  let url: URL;
  try {
    url = new URL(data);
  } catch (e) {
    console.log(`Error with scanned url: ${e}`);
    return 'Invalid url';
  }

  const urlArg = url.searchParams;

  const requiredArguments = [
    'client_id',
    'redirect_uri',
    'login_hint',
    'nonce',
    'response_type',
    'scope',
  ];

  // Check if all required arguments are present
  for (const arg of requiredArguments) {
    if (!urlArg.has(arg)) {
      return `Missing argument ${arg}`;
    }
  }

  // Check if the response respects openid standard
  if (urlArg.get('response_type') !== 'id_token') {
    return 'Invalid response type';
  }

  if (!(urlArg.get('scope')!.includes('openid') && urlArg.get('scope')!.includes('profile'))) {
    return 'Invalid scope';
  }

  if (urlArg.has('response_mode')) {
    if (
      !(
        urlArg.get('response_mode')!.includes('query') ||
        urlArg.get('response_mode')!.includes('fragment')
      )
    ) {
      return 'Invalid response mode';
    }
  }

  if (urlArg.get('login_hint') !== laoId.toString()) {
    console.log(`Scanned lao id: ${urlArg.get('login_hint')}, current lao id: ${laoId}`);
    return 'Invalid lao id';
  }

  return undefined;
};

/**
 * Send an auth request to the server
 * @param data the scanned data (url)
 * @param laoId the current lao id
 * @param generateToken function to deterministically generate a long term token
 * @returns A promise that resolves when the message has been sent
 * @throws ProtocolError if the scanned info is invalid
 * @throws NetworkError if message could not be sent correctly
 */
export const sendAuthRequest = async (
  data: string,
  laoId: Hash,
  generateToken: (laoId: Hash, clientId: Hash | undefined) => Promise<PopToken>,
) => {
  const errMsg = verifyScannedInfo(data, laoId);
  if (errMsg) {
    throw new ProtocolError(errMsg);
  }

  const url = new URL(data);
  const urlArg = url.searchParams;

  return sendPopchaAuthRequest(
    urlArg.get('client_id')!,
    urlArg.get('nonce')!,
    url.host,
    urlArg.get('state'),
    urlArg.get('response_mode'),
    laoId,
    generateToken,
  );
};
