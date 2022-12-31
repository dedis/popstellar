import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { MeetingReactContext, MEETING_FEATURE_IDENTIFIER } from 'features/meeting/interface';

import { MeetingHooks } from '../index';

const contextValue = {
  [MEETING_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    useConnectedToLao: () => false,
  } as MeetingReactContext,
};

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('Meeting hooks', () => {
  describe('useuseCurrentLaoIdAssertCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => MeetingHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoId);
    });
  });

  describe('useConnectedToLao', () => {
    it('should return the current connection state', () => {
      const { result } = renderHook(() => MeetingHooks.useConnectedToLao(), { wrapper });
      expect(result.current).toBeFalse();
    });
  });
});
