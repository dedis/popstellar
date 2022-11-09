import { describe } from '@jest/globals';
import { configureStore } from '@reduxjs/toolkit';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers } from 'redux';

import { mockLao, mockLaoIdHash } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HOME_FEATURE_IDENTIFIER, HomeFeature, HomeReactContext } from 'features/home/interface';
import { LaoHooks } from 'features/lao/hooks';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import { HomeHooks } from '../index';

const requestCreateLao = jest.fn();
const addLaoServerAddress = jest.fn();
const getLaoChannel = jest.fn();
const connectToTestLao = jest.fn();
const LaoList = jest.fn();
const hasSeed = jest.fn();
const resubscribeToLao = jest.fn();
const forgetSeed = jest.fn();
const homeNavigationScreens: HomeFeature.HomeScreen[] = [
  {
    Component: LaoList,
    id: 'x' as HomeFeature.HomeScreen['id'],
    title: 'X',
  },
];

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    requestCreateLao,
    addLaoServerAddress,
    connectToTestLao,
    useLaoList: LaoHooks.useLaoList,
    LaoList,
    homeNavigationScreens,
    getLaoChannel,
    useCurrentLaoId: LaoHooks.useCurrentLaoId,
    hasSeed,
    useDisconnectFromLao: () => () => {},
    getLaoById: () => mockLao,
    resubscribeToLao,
    forgetSeed,
  } as HomeReactContext,
};

// setup mock store
const mockStore = configureStore({ reducer: combineReducers(laoReducer) });
mockStore.dispatch(setCurrentLao(mockLao.toState()));

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <Provider store={mockStore}>
    <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
  </Provider>
);

describe('Home hooks', () => {
  describe('useCreateLao', () => {
    it('should return the function for creating a lao', () => {
      const { result } = renderHook(() => HomeHooks.useRequestCreateLao(), { wrapper });
      expect(result.current).toEqual(requestCreateLao);
    });
  });

  describe('useAddLaoServerAddress', () => {
    it('should return the function for adding a lao address', () => {
      const { result } = renderHook(() => HomeHooks.useAddLaoServerAddress(), { wrapper });
      expect(result.current).toEqual(addLaoServerAddress);
    });
  });

  describe('useConnectToTestLao', () => {
    it('should return the function for connecting to the test lao', () => {
      const { result } = renderHook(() => HomeHooks.useConnectToTestLao(), { wrapper });
      expect(result.current).toEqual(connectToTestLao);
    });
  });

  describe('useLaoList', () => {
    it('should return the list of current laos', () => {
      const { result } = renderHook(() => HomeHooks.useLaoList(), { wrapper });
      expect(result.current).toEqual([mockLao]);
    });
  });

  describe('useLaoListComponent', () => {
    it('should return the lao list component', () => {
      const { result } = renderHook(() => HomeHooks.useLaoListComponent(), { wrapper });
      expect(result.current).toEqual(LaoList);
    });
  });

  describe('useMainNavigationScreens', () => {
    it('should return the main navigation screens', () => {
      const { result } = renderHook(() => HomeHooks.useHomeNavigationScreens(), { wrapper });
      expect(result.current).toEqual(homeNavigationScreens);
    });
  });

  describe('useCurrentLaoId', () => {
    it('should return the the current lao id', () => {
      const { result } = renderHook(() => HomeHooks.useCurrentLaoId(), { wrapper });
      expect(result.current).toEqual(mockLaoIdHash);
    });
  });

  describe('useGetLaoChannel', () => {
    it('should return the function for getting a channel by lao id', () => {
      const { result } = renderHook(() => HomeHooks.useGetLaoChannel(), { wrapper });
      expect(result.current).toEqual(getLaoChannel);
    });
  });

  describe('useResubscribeToLao', () => {
    it('should return the function for resubscribing to a lao', () => {
      const { result } = renderHook(() => HomeHooks.useResubscribeToLao(), { wrapper });
      expect(result.current).toEqual(resubscribeToLao);
    });
  });

  describe('useForgetSeed', () => {
    it('should return the function for forgetting the current seed', () => {
      const { result } = renderHook(() => HomeHooks.useForgetSeed(), { wrapper });
      expect(result.current).toEqual(forgetSeed);
    });
  });
});
