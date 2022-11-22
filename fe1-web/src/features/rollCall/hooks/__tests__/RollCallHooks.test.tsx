import { describe } from '@jest/globals';
import { configureStore } from '@reduxjs/toolkit';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLao, mockLaoId, org } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash, Timestamp } from 'core/objects';
import { addEvent, eventReducer, makeEventByTypeSelector } from 'features/events/reducer';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';
import { mockRollCall, mockRollCallState } from 'features/rollCall/__tests__/utils';
import { ROLLCALL_FEATURE_IDENTIFIER, RollCallReactContext } from 'features/rollCall/interface';
import { CreateRollCall } from 'features/rollCall/network/messages';
import { RollCall, RollCallStatus } from 'features/rollCall/objects';
import { addRollCall, rollCallReducer } from 'features/rollCall/reducer';

import { RollCallHooks } from '../index';

// region mock data

const mockLaoName2 = 'MyLao';
const mockLaoCreationTime2 = new Timestamp(1600009000);
const mockLaoId2: Hash = Hash.fromStringArray(
  org.toString(),
  mockLaoCreationTime2.toString(),
  mockLaoName2,
);

const mockRollCallName3 = 'myRollCall3';
const mockRollCallLocation3 = 'location3';
const mockRollCallTimestampCreation3 = new Timestamp(1620255700);
const mockRollCallTimestampStart3 = new Timestamp(1620255800);
const mockRollCallTimestampEnd3 = new Timestamp(1620357900);

const mockRollCall3 = new RollCall({
  id: CreateRollCall.computeRollCallId(
    mockLaoId2,
    mockRollCallTimestampCreation3,
    mockRollCallName3,
  ),
  start: mockRollCallTimestampStart3,
  end: mockRollCallTimestampEnd3,
  name: mockRollCallName3,
  location: mockRollCallLocation3,
  creation: mockRollCallTimestampCreation3,
  proposedStart: mockRollCallTimestampStart3,
  proposedEnd: mockRollCallTimestampEnd3,
  status: RollCallStatus.CREATED,
  attendees: [],
});

const mockGenerateToken = jest.fn();
const mockHasSeed = jest.fn();

const contextValue = {
  [ROLLCALL_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => false,
    makeEventByTypeSelector,
    generateToken: mockGenerateToken,
    hasSeed: mockHasSeed,
  } as RollCallReactContext,
};

const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...eventReducer,
    ...rollCallReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));
mockStore.dispatch(
  addEvent(mockLaoId, {
    eventType: RollCall.EVENT_TYPE,
    id: mockRollCall.id.valueOf(),
    start: mockRollCall.start.valueOf(),
    end: mockRollCall.end?.valueOf(),
  }),
);
mockStore.dispatch(addRollCall(mockRollCallState));

mockStore.dispatch(
  addEvent(mockLaoId2, {
    eventType: RollCall.EVENT_TYPE,
    id: mockRollCall3.id.valueOf(),
    start: mockRollCall3.start.valueOf(),
    end: mockRollCall3.end?.valueOf(),
  }),
);
mockStore.dispatch(addRollCall(mockRollCall3.toState()));

// endregion

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <Provider store={mockStore}>
    <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
  </Provider>
);

describe('RollCallHooks', () => {
  describe('useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => RollCallHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoId);
    });
  });

  describe('useConnectedToLao', () => {
    it('should return whether currently connected to a lao', () => {
      const { result } = renderHook(() => RollCallHooks.useConnectedToLao(), { wrapper });
      expect(result.current).toBeFalse();
    });
  });

  describe('useGenerateToken', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => RollCallHooks.useGenerateToken(), { wrapper });
      expect(result.current).toBe(mockGenerateToken);
    });
  });

  describe('hasSeed', () => {
    it('should return the correct function', () => {
      const { result } = renderHook(() => RollCallHooks.useHasSeed(), { wrapper });
      expect(result.current).toBe(mockHasSeed);
    });
  });

  describe('RollCallHook.useRollCallsByLaoId', () => {
    it('should return the correct value', () => {
      const { result } = renderHook(() => RollCallHooks.useRollCallsByLaoId(mockLaoId), {
        wrapper,
      });
      expect(result.current).toEqual({
        [mockRollCallState.id]: mockRollCall,
      });
    });
  });
});
