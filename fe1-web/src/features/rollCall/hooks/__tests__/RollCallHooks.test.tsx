import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { mockLao, mockLaoId, mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { addEvent, eventsReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { connectToLao, laoReducer } from 'features/lao/reducer';
import { mockRollCall, mockRollCallState } from 'features/rollCall/__tests__/utils';
import { RollCallReactContext, ROLLCALL_FEATURE_IDENTIFIER } from 'features/rollCall/interface';
import { RollCall } from 'features/rollCall/objects';
import { addRollCall } from 'features/rollCall/reducer';

import { RollCallHooks } from '../index';

const mockGenerateToken = jest.fn();
const mockHasSeed = jest.fn();

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
    makeEventByTypeSelector,
    generateToken: mockGenerateToken,
    hasSeed: mockHasSeed,
  } as RollCallReactContext,
};

const mockStore = createStore(combineReducers({ ...laoReducer, ...eventsReducer }));
mockStore.dispatch(connectToLao(mockLao.toState()));
mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: RollCall.EVENT_TYPE,
    id: mockRollCallState.id.valueOf(),
    start: mockRollCall.start.valueOf(),
    end: mockRollCall.end.valueOf(),
  }),
);
mockStore.dispatch(addRollCall(mockRollCallState));

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
