import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { MeetingReactContext, MEETING_FEATURE_IDENTIFIER } from 'features/meeting/interface';

import { MeetingHooks } from '../index';

const contextValue = {
  [MEETING_FEATURE_IDENTIFIER]: {
    useAssertCurrentLaoId: () => mockLaoIdHash,
  } as MeetingReactContext,
};

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('Meeting hooks', () => {
  describe('MeetingHooks.useAssertCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => MeetingHooks.useAssertCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });
});
