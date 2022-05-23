import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { mockLao, mockLaoId } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import {
  ConnectReactContext,
  CONNECT_FEATURE_IDENTIFIER,
} from 'features/connect/interface/Configuration';
import { LaoHooks } from 'features/lao/hooks';
import { connectToLao, laoReducer } from 'features/lao/reducer';

import { ConnectHooks } from '../ConnectHooks';

const addLaoServerAddress = jest.fn();
const getLaoChannel = jest.fn();

const contextValue = {
  [CONNECT_FEATURE_IDENTIFIER]: {
    addLaoServerAddress,
    useCurrentLaoId: LaoHooks.useCurrentLaoId,
    getLaoChannel,
  } as ConnectReactContext,
};

// setup mock store
const mockStore = createStore(combineReducers(laoReducer));
mockStore.dispatch(connectToLao(mockLao.toState()));

const wrapper = ({ children }: { children: React.ReactChildren }) => (
  <Provider store={mockStore}>
    <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
  </Provider>
);

describe('Connect hooks', () => {
  describe('useAddLaoServerAddress', () => {
    it('should return the function for adding a lao server address', () => {
      const { result } = renderHook(() => ConnectHooks.useAddLaoServerAddress(), { wrapper });
      expect(result.current).toEqual(addLaoServerAddress);
    });
  });

  describe('useCurrentLaoId', () => {
    it('should return the the current lao id', () => {
      const { result } = renderHook(() => ConnectHooks.useCurrentLaoId(), { wrapper });
      expect(result.current?.valueOf()).toEqual(mockLaoId);
    });
  });

  describe('useGetLaoChannel', () => {
    it('should return the function for getting a channel by lao id', () => {
      const { result } = renderHook(() => ConnectHooks.useGetLaoChannel(), { wrapper });
      expect(result.current).toEqual(getLaoChannel);
    });
  });
});
