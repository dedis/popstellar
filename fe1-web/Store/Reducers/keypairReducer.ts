import { AnyAction } from 'redux';
import { KeyPairData } from '../objects';

const initialState: KeyPairData = { publicKey: undefined, privateKey: undefined };

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
 * @returns {{secKey: ([]|*[]|string), pubKey: ([]|*[]|string)}|{secKey: [], pubKey: []}}
 * new key pair if action is valid, old key pair otherwise
 */
function keypairReducer(state: KeyPairData = initialState, action: AnyAction): KeyPairData {
  if (action.type === 'SET_KEYPAIR') {
    const nextState = {
      ...state,
      publicKey: action.value.publicKey,
      privateKey: action.value.privateKey,
    };
    return nextState || state;
  }
  return state;
}

export default keypairReducer;
