import { Store } from 'redux';

import { PublicKey } from 'core/objects';

import { getElectionKeyByElectionId } from './ElectionKeyReducer';

/**
 * Watches the redux store for new message#witness messages for lao#greet messages since they only
 * become valid after they are witnessed by the corresponding frontend
 * @remark Implemented analogous to 'makeMessageStoreWatcher'
 * @param store The redux store to watch
 * @param laoGreetSignatureHandler The function to call when a signature is added to a lao#greet message
 */
export const makeElectionKeyStoreWatcher =
  (electionId: string, store: Store, callback: (electionKey: PublicKey) => void) => () => {
    const electionKey = getElectionKeyByElectionId(electionId, store.getState());

    if (electionKey) {
      callback(electionKey);
    }
  };
