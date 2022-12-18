import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import {
  mockLao,
  mockLaoId,
  messageRegistryInstance,
  mockReduxAction,
  mockKeyPair,
} from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { EvotingReactContext, EVOTING_FEATURE_IDENTIFIER } from 'features/evoting/interface';

import { EvotingHooks } from '../index';

const onConfirmEventCreation = jest.fn();

const contextValue = {
  [EVOTING_FEATURE_IDENTIFIER]: {
    useCurrentLao: () => mockLao,
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => false,
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

  describe('useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => EvotingHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoId);
    });
  });

  describe('useConnectedToLao', () => {
    it('should return whether currently connected to a lao', () => {
      const { result } = renderHook(() => EvotingHooks.useConnectedToLao(), { wrapper });
      expect(result.current).toBeFalse();
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
