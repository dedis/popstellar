import { Store } from 'redux';

import { Hash } from 'core/objects';

import { getElectionKeyMessageIdByElectionId } from './ElectionKeyReducer';

/**
 * Watches the redux store for new election#key message for the given election id
 * @remark Implemented analogous to 'makeMessageStoreWatcher'
 * @param store The redux store to watch
 * @param callback The function to call when a the corresponding election#key message is received
 */
export const makeElectionKeyStoreWatcher =
  (electionId: string, store: Store, callback: (messageId: Hash) => void) => () => {
    const messageId = getElectionKeyMessageIdByElectionId(electionId, store.getState());

    if (messageId) {
      callback(messageId);
    }
  };
