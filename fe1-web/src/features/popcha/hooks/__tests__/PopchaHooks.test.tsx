import * as assert from 'assert';

import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';

import { mockLaoId, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash } from 'core/objects';

import { POPCHA_FEATURE_IDENTIFIER, PopchaReactContext } from '../../interface';
import { PopchaHooks } from '../PopchaHooks';

const contextValue = {
  [POPCHA_FEATURE_IDENTIFIER]: {
    useCurrentLaoId: () => mockLaoId,
    generateToken: () => Promise.resolve(mockPopToken),
  } as PopchaReactContext,
};

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
);

describe('PoPcha hooks', () => {
  describe('useCurrentLaoId', () => {
    it('should return the current lao id', () => {
      const { result } = renderHook(() => PopchaHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoId);
    });
  });
  describe('generateToken', () => {
    it('should return a token', async () => {
      const { result } = renderHook(() => PopchaHooks.useGenerateToken(), {
        wrapper,
      });
      await result
        // call with mock params
        .current({} as Hash, undefined)
        .then((token) => expect(token).toEqual(mockPopToken))
        .catch(assert.fail);
    });
  });
});
