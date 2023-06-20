import { Hash, PopToken, ProtocolError } from 'core/objects';

import { sendPopchaAuthRequest } from '../network/PopchaMessageApi';

/**
 * Get the argument from the url
 * @param url the url
 * @param arg the argument
 * @throws ProtocolError if the argument is missing
 */
const getArg = (url: URL, arg: string) => {
  const value = url.searchParams.get(arg);
  if (!value) {
    throw new ProtocolError(`Missing argument ${arg}`);
  }
  return value;
};

/**
 * Verify the scanned info (url)
 * @param data the scanned data
 * @param laoId the lao id
 * @throws ProtocolError if the scanned data is invalid
 */
const verifyScannedInfo = (data: string, laoId: Hash) => {
  let url: URL;
  try {
    url = new URL(data);
  } catch (e) {
    throw new ProtocolError(`Invalid URL: ${e}`);
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
      throw new ProtocolError(`Missing argument ${arg}`);
    }
  }

  // Check if the response respects openid standard
  if (getArg(url, 'response_type') !== 'id_token') {
    throw new ProtocolError('Invalid response type');
  }

  if (!(getArg(url, 'scope').includes('openid') && getArg(url, 'scope').includes('profile'))) {
    throw new ProtocolError('Invalid scope');
  }

  const responseMode = url.searchParams.get('response_mode');

  if (responseMode) {
    if (!(responseMode.includes('query') || responseMode.includes('fragment'))) {
      throw new ProtocolError('Invalid response mode');
    }
  }

  if (getArg(url, 'login_hint') !== laoId.toString()) {
    console.info(`Scanned lao id: ${getArg(url, 'login_hint')}, current lao id: ${laoId}`);
    throw new ProtocolError('Invalid lao id');
  }
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
  verifyScannedInfo(data, laoId);

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
