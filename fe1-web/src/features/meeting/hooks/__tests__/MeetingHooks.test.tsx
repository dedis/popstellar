import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { MeetingReactContext, MEETING_FEATURE_IDENTIFIER } from 'features/meeting/interface';

import { MeetingHooks } from '../index';

const contextValue = {
  [MEETING_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoIdHash,
  } as MeetingReactContext,
};

const wrapper = ({ children }: { children: React.ReactChildren }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('Meeting hooks', () => {
  describe('MeetingHooks.useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => MeetingHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });
});
