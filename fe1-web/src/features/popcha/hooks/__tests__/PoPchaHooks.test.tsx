import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';

import { POPCHA_FEATURE_IDENTIFIER, PoPchaReactContext } from '../../interface';
import { PoPchaHooks } from '../PoPchaHooks';

const contextValue = {
  [POPCHA_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
  } as PoPchaReactContext,
};

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('PoPcha hooks', () => {
  describe('useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => PoPchaHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoId);
    });
  });
});
