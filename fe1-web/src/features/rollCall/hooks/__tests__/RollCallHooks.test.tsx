import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { mockLao, mockLaoIdHash, mockRollCallState } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { addEvent, eventsReducer, makeEventSelector } from 'features/events/reducer';
import { connectToLao, laoReducer } from 'features/lao/reducer';
import { RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from 'features/rollCall/interface';
import { mockLaoId } from 'features/wallet/objects/DummyWallet';

import { RollCallHooks } from '../index';

const mockGenerateToken = jest.fn();
const mockHasSeed = jest.fn();

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    makeEventSelector,
    generateToken: mockGenerateToken,
    hasSeed: mockHasSeed,
  } as RollCallReactContext,
};

const mockStore = createStore(combineReducers({ ...laoReducer, ...eventsReducer }));
mockStore.dispatch(connectToLao(mockLao.toState()));
mockStore.dispatch(addEvent(mockLaoId, mockRollCallState));

const wrapper = ({ children }: { children: React.ReactChildren }) => (
  <Provider store={mockStore}>
    <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
  </Provider>
);

describe('RollCallHooks', () => {
  describe('RollCallHook.useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => RollCallHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });

  describe('RollCallHook.useEventSelector', () => {
    it('should return the undefined if there is no entry for the given lao id', () => {
      const { result } = renderHook(
        () => RollCallHooks.useEventSelector('someInexistentLao', mockRollCallState.id),
        { wrapper },
      );
      expect(result.current).toBeUndefined();
    });

    it('should return the undefined if there is no event for the given id', () => {
      const { result } = renderHook(
        () => RollCallHooks.useEventSelector(mockLaoId, 'someInexistentEventId'),
        { wrapper },
      );
      expect(result.current).toBeUndefined();
    });

    it('should return the event if there is an event for the given ids', () => {
      const { result } = renderHook(
        () => RollCallHooks.useEventSelector(mockLaoId, mockRollCallState.id),
        {
          wrapper,
        },
      );
      // the 'end' field
      expect(result.current?.toState()).toEqual(mockRollCallState);
    });
  });

  describe('RollCallHook.useGenerateToken', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => RollCallHooks.useGenerateToken(), { wrapper });
      expect(result.current).toBe(mockGenerateToken);
    });
  });

  describe('RollCallHook.hasSeed', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => RollCallHooks.useHasSeed(), { wrapper });
      expect(result.current).toBe(mockHasSeed);
    });
  });
});
