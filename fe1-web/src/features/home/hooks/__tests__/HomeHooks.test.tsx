import { describe } from '@jest/globals';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, createStore } from 'redux';

import { mockLao } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { HomeFeature, HomeReactContext, HOME_FEATURE_IDENTIFIER } from 'features/home/interface';
import { LaoHooks } from 'features/lao/hooks';
import { addLao, laoReducer } from 'features/lao/reducer';

import { HomeHooks } from '../index';

const requestCreateLao = jest.fn();
const addLaoServerAddress = jest.fn();
const connectToTestLao = jest.fn();
const LaoList = jest.fn();
const mainNavigationScreens: HomeFeature.Screen[] = [
  { Component: LaoList, id: 'x', title: 'X', order: 2 },
];

const contextValue = {
  [HOME_FEATURE_IDENTIFIER]: {
    requestCreateLao,
    addLaoServerAddress,
    connectToTestLao,
    useLaoList: LaoHooks.useLaoList,
    LaoList,
    mainNavigationScreens,
  } as HomeReactContext,
};

// setup mock store
const mockStore = createStore(combineReducers(laoReducer));
mockStore.dispatch(addLao(mockLao.toState()));

const wrapper = ({ children }: { children: React.ReactChildren }) => (
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
      const { result } = renderHook(() => HomeHooks.useMainNavigationScreens(), { wrapper });
      expect(result.current).toEqual(mainNavigationScreens);
    });
  });
});
