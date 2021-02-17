import { AnyAction } from 'redux';
import { KeyPair, KeyPairState } from 'model/objects';
import { ActionKeyPairReducer } from '../Actions';

const initialState: KeyPairState | null = null;

/**
 * Reducer to store a set of public/private key
 *
 * Action types:
 *  - SET_KEYPAIR: set a key pair in local storage
 *
 *  Note: action is a JsObject of the form { type: <ACTION_TYPE> } where
 *  <ACTION_TYPE> is a string
 *
 * @param state JsObject containing the keys to store
 * @param action action to be executed by the reducer
 * @returns new key pair if action is valid, old key pair otherwise
 */
export function keyPairReducer(
  state: KeyPairState | null = initialState, action: AnyAction,
): KeyPairState | null {
  try {
    if (action.type === ActionKeyPairReducer.SET_KEYPAIR) {
      if (action.value === undefined || action.value === null) {
        console.log('KeyPair storage was set to: null');
        return null;
      }
      const kp = KeyPair.fromState(action.value).toState();
      console.log(`KeyPair storage was update with public key: ${kp.publicKey.toString()}`);
      return Object.freeze(kp);
    }
  } catch (e) {
    console.exception('Could not update KeyPair state due to exception', e);
  }

  console.log(`KeyPair storage stayed unchanged after action : '${action.type}'`);
  return state;
}
