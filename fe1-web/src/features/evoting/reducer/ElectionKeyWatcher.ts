import { Store } from 'redux';

import { Hash, PublicKey } from 'core/objects';

import { getElectionKeyByElectionId } from './ElectionKeyReducer';

/**
 * Watches the redux store for new election#key message for the given election id
 * @remark Implemented analogous to 'makeMessageStoreWatcher'
 * @param store The redux store to watch
 * @param callback The function to call when a the corresponding election#key message is received
 */
export const makeElectionKeyStoreWatcher =
  (
    electionId: string,
    store: Store,
    callback: (electionKey: { electionKey: PublicKey; messageId: Hash }) => void,
  ) =>
  () => {
    const electionKey = getElectionKeyByElectionId(electionId, store.getState());

    if (electionKey) {
      callback(electionKey);
    }
  };
