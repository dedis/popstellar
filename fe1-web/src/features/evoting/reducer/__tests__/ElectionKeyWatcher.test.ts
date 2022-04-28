import { combineReducers, createStore } from 'redux';

import { Hash } from 'core/objects';
import { mockElectionId } from 'features/evoting/__tests__/utils';

import { addElectionKeyMessage } from '../ElectionKeyReducer';
import { makeElectionKeyStoreWatcher } from '../ElectionKeyWatcher';
import { electionKeyReducer } from '../index';

describe('makeElectionKeyStoreWatcher', () => {
  it('does not trigger the callback if an unrelated election key is added', () => {
    const mockStore = createStore(combineReducers({ ...electionKeyReducer }));
    const callback = jest.fn();

    const watcher = makeElectionKeyStoreWatcher(mockElectionId.valueOf(), mockStore, callback);
    mockStore.subscribe(watcher);

    mockStore.dispatch(addElectionKeyMessage({ electionId: 'someId', messageId: 'someId' }));

    expect(callback).not.toHaveBeenCalled();
  });

  it('does trigger the callback if an the correct election key is added', () => {
    const mockStore = createStore(combineReducers({ ...electionKeyReducer }));
    const callback = jest.fn();

    const watcher = makeElectionKeyStoreWatcher(mockElectionId.valueOf(), mockStore, callback);
    mockStore.subscribe(watcher);

    const mockMessageId = 'someId';
    mockStore.dispatch(
      addElectionKeyMessage({ electionId: mockElectionId.valueOf(), messageId: mockMessageId }),
    );

    expect(callback).toHaveBeenCalledWith(new Hash(mockMessageId));
    expect(callback).toHaveBeenCalledTimes(1);
  });
});
