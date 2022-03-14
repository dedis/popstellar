import React from 'react';
import { describe } from '@jest/globals';
import FeatureContext from 'core/contexts/FeatureContext';
import { mockLao, mockLaoIdHash, mockMessageRegistry, mockReduxAction } from '__tests__/utils';
import { renderHook } from '@testing-library/react-hooks';
import { EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';
import { EvotingHooks } from '../index';

const onConfirmEventCreation = jest.fn();

const contextValue = {
  [EVOTING_FEATURE_IDENTIFIER]: {
    useCurrentLao: () => mockLao,
    useCurrentLaoId: () => mockLaoIdHash,
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventById: () => undefined,
    messageRegistry: mockMessageRegistry,
    onConfirmEventCreation,
  },
};

const wrapper = ({ children }: { children: React.ReactChildren }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('E-Voting hooks', () => {
  describe('EvotingHooks.useCurrentLao', () => {
    it('should return the current lao', () => {
      const { result } = renderHook(() => EvotingHooks.useCurrentLao(), { wrapper });
      expect(result.current).toEqual(mockLao);
    });
  });

  describe('EvotingHooks.useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => EvotingHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });

  describe('EvotingHooks.useOnConfirmEventCreation', () => {
    it('should return the onConfirmEventCreation config option', () => {
      const { result } = renderHook(() => EvotingHooks.useOnConfirmEventCreation(), { wrapper });
      expect(result.current).toEqual(onConfirmEventCreation);
    });
  });
});
