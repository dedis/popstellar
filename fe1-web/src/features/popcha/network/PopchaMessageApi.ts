/**
 * Contains all functions to send popcha related messages.
 * */

import { publish } from 'core/network';
import { getPopchaAuthenticationChannel, Hash } from 'core/objects';

import { PopchaAuthMsg } from './messages/PopchaAuthMsg';

/**
 * Sends a message to the server to authenticate that the user belongs to the lao
 * @param client_id id of the client
 * @param nonce nonce
 * @param identifier long term identifier (public key)
 * @param identifier_proof signature of the nonce
 * @param popcha_address address of the popcha server
 * @param state state
 * @param laoId id of the lao
 * @returns A promise that resolves when the message has been sent
 */
export const sendPopchaAuthRequest = (
  client_id: string,
  nonce: string,
  identifier: string,
  identifier_proof: string,
  popcha_address: string,
  state: string,
  laoId: Hash,
): Promise<void> => {
  const popchaChannel = getPopchaAuthenticationChannel(laoId);
  const message = new PopchaAuthMsg({
    client_id: client_id,
    nonce: nonce,
    identifier: identifier,
    identifier_proof: identifier_proof,
    popcha_address: popcha_address,
    state: state,
  });

  return publish(popchaChannel, message);
};
