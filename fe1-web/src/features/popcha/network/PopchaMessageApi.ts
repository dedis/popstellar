/**
 * Contains all functions to send popcha related messages.
 * */

import { publish } from 'core/network';
import { Base64UrlData, getPopchaAuthenticationChannel, Hash } from 'core/objects';

import { generateToken } from '../../wallet/objects';
import { PopchaAuthMsg } from './messages/PopchaAuthMsg';

/**
 * Sends a message to the server to authenticate that the user belongs to the lao
 * @param client_id id of the client
 * @param nonce nonce
 * @param popcha_address address of the popcha server
 * @param state state
 * @param laoId id of the lao
 * @returns A promise that resolves when the message has been sent
 */
export const sendPopchaAuthRequest = (
  client_id: Hash,
  nonce: string,
  popcha_address: string,
  state: string,
  laoId: Hash,
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
      popcha_address: popcha_address /* to add response_mode */,
      state: state,
    });
    return publish(popchaChannel, message);
  });
};
