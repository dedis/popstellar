/**
 * Contains all functions to send popcha related messages.
 * */

import { publish } from 'core/network';
import { Base64UrlData, getPopchaAuthenticationChannel, Hash, PopToken } from 'core/objects';

import { PopchaAuthMsg } from './messages/PopchaAuthMsg';

/**
 * Sends a message to the server to authenticate that the user belongs to the lao
 * @param client_id id of the client
 * @param nonce nonce
 * @param popcha_address address of the popcha server
 * @param state state
 * @param response_mode response mode (optional) ('query' or 'fragment')
 * @param laoId id of the lao
 * @param generateToken function to generate a long term token
 * @returns A promise that resolves when the message has been sent
 */
export const sendPopchaAuthRequest = (
  client_id: Hash,
  nonce: string,
  popcha_address: string,
  state: string | null,
  response_mode: string | null,
  laoId: Hash,
  generateToken: (laoId: Hash, clientId: Hash | undefined) => Promise<PopToken>,
): Promise<void> => {
  const token = generateToken(laoId, client_id);
  return token.then((t) => {
    const signedToken = t.privateKey.sign(new Base64UrlData(nonce));
    const popchaChannel = getPopchaAuthenticationChannel(laoId);
    const message = new PopchaAuthMsg({
      client_id: client_id.toString(),
      nonce: nonce,
      identifier: t.publicKey.toString(),
      identifier_proof: signedToken.toString(),
      popcha_address: popcha_address,
      state: state || undefined,
      response_mode: response_mode || undefined,
    });
    return publish(popchaChannel, message);
  });
};
