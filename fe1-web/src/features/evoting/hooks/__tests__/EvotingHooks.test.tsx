import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import {
  mockLao,
  mockLaoIdHash,
  messageRegistryInstance,
  mockReduxAction,
  mockKeyPair,
  mockLaoId,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EvotingReactContext, EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';

import { EvotingHooks } from '../index';

const onConfirmEventCreation = jest.fn();

const contextValue = {
  [EVOTING_FEATURE_IDENTIFIER]: {
    useCurrentLao: () => mockLao,
    useAssertCurrentLaoId: () => mockLaoIdHash,
    addEvent: () => mockReduxAction,
    updateEvent: () => mockReduxAction,
    getEventById: () => undefined,
    messageRegistry: messageRegistryInstance,
    onConfirmEventCreation,
    useLaoOrganizerBackendPublicKey: () => mockKeyPair.publicKey,
  } as EvotingReactContext,
};

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('EvotingHooks', () => {
  describe('useCurrentLao', () => {
    it('should return the current lao', () => {
      const { result } = renderHook(() => EvotingHooks.useCurrentLao(), { wrapper });
      expect(result.current).toEqual(mockLao);
    });
  });

  describe('useAssertCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => EvotingHooks.useAssertCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });

  describe('EvotingHooks.useLaoOrganizerBackendPublicKey', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => EvotingHooks.useLaoOrganizerBackendPublicKey(mockLaoId), {
        wrapper,
      });
      expect(result.current).toEqual(mockKeyPair.publicKey);
    });
  });
});
