import { describe } from '@jest/globals';
import { configureStore } from '@reduxjs/toolkit';
import { renderHook } from '@testing-library/react-hooks';
import React from 'react';
import { Provider } from 'react-redux';
import { combineReducers, Store } from 'redux';

import { mockKeyPair, mockLao, serializedMockLaoId, mockPopToken } from '__tests__/utils';
import FeatureContext from 'core/contexts/FeatureContext';
import { keyPairReducer, setKeyPair } from 'core/keypair';
import { encodeLaoConnectionForQRCode } from 'features/home/functions';
import { NoCurrentLaoError } from 'features/lao/errors/NoCurrentLaoError';
import { LAO_FEATURE_IDENTIFIER, LaoFeature, LaoReactContext } from 'features/lao/interface';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';

import { LaoHooks } from '../LaoHooks';

const EventList = jest.fn();
const CreateEventButton = jest.fn();

const laoNavigationScreens: LaoFeature.LaoScreen[] = [];
const organizerNavigationScreens: LaoFeature.LaoEventScreen[] = [];

const contextValue = {
  [LAO_FEATURE_IDENTIFIER]: {
    encodeLaoConnectionForQRCode,
    laoNavigationScreens,
    eventsNavigationScreens: organizerNavigationScreens,
    EventList,
    CreateEventButton,
  } as LaoReactContext,
};

// setup mock store
const mockStore = configureStore({
  reducer: combineReducers({
    ...laoReducer,
    ...keyPairReducer,
  }),
});
mockStore.dispatch(setCurrentLao(mockLao));

// setup mock store
const emptyMockStore = configureStore({ reducer: combineReducers(laoReducer) });

const wrapper =
  (store: Store) =>
  ({ children }: { children: React.ReactNode }) =>
    (
      <Provider store={store}>
        <FeatureContext.Provider value={contextValue}>{children}</FeatureContext.Provider>
      </Provider>
    );

describe('LaoHooks', () => {
  describe('useCurrentLao', () => {
    it('should return the current lao if there is one', () => {
      const { result } = renderHook(() => LaoHooks.useCurrentLao(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toEqual(mockLao);
    });
  });

  describe('useCurrentLaoId', () => {
    it('should return the current lao id if there is one', () => {
      const { result } = renderHook(() => LaoHooks.useCurrentLaoId(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current?.valueOf()).toEqual(serializedMockLaoId);
    });

    it('should throw if there is no current lao', () => {
      const { result } = renderHook(() => LaoHooks.useCurrentLaoId(), {
        wrapper: wrapper(emptyMockStore),
      });
      expect(result.error).toBeInstanceOf(NoCurrentLaoError);
    });
  });

  describe('useConnectedToLao', () => {
    it('returns true if currently connected to a lao', () => {
      const { result } = renderHook(() => LaoHooks.useConnectedToLao(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toBeTrue();
    });

    it('returns false if in offline mode (there is a current lao but no connection to it)', () => {
      // setup mock store
      const offlineMockStore = configureStore({ reducer: combineReducers(laoReducer) });
      offlineMockStore.dispatch(setCurrentLao(mockLao, false));

      const { result } = renderHook(() => LaoHooks.useConnectedToLao(), {
        wrapper: wrapper(offlineMockStore),
      });
      expect(result.current).toBeFalse();
    });

    it('returns undefined if there is no current lao', () => {
      const { result } = renderHook(() => LaoHooks.useConnectedToLao(), {
        wrapper: wrapper(emptyMockStore),
      });
      expect(result.current).toBeUndefined();
    });
  });

  describe('useEncodeLaoConnectionForQRCode', () => {
    it('should return the function for encoding a connection', () => {
      const { result } = renderHook(() => LaoHooks.useEncodeLaoConnectionForQRCode(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toEqual(encodeLaoConnectionForQRCode);
    });
  });

  describe('useEventListComponent', () => {
    it('should return the current event list component', () => {
      const { result } = renderHook(() => LaoHooks.useEventListComponent(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toBe(EventList);
    });
  });

  describe('useCreateEventButtonComponent', () => {
    it('should return the current event list component', () => {
      const { result } = renderHook(() => LaoHooks.useCreateEventButtonComponent(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toBe(CreateEventButton);
    });
  });

  describe('useIsLaoOrganizer', () => {
    it('should return true if the current user is the organizer', () => {
      // make us the organizer
      mockStore.dispatch(setKeyPair(mockKeyPair.toState()));

      const { result } = renderHook(() => LaoHooks.useIsLaoOrganizer(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toBeTrue();
    });

    it('should return false if the current user is not the organizer', () => {
      // make us an attendee (i.e. set a different key pair)
      mockStore.dispatch(setKeyPair(mockPopToken.toState()));

      const { result } = renderHook(() => LaoHooks.useIsLaoOrganizer(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toBeFalse();
    });
  });

  describe('useLaoList', () => {
    it('should return the list of stored laos', () => {
      const { result } = renderHook(() => LaoHooks.useLaoList(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toEqual([mockLao]);
    });
  });

  describe('useLaoMap', () => {
    it('should return all stored laos in a mapping from ids to laos', () => {
      const { result } = renderHook(() => LaoHooks.useLaoMap(), {
        wrapper: wrapper(mockStore),
      });
      expect(result.current).toEqual({ [serializedMockLaoId]: mockLao });
    });
  });

  describe('useLaoNavigationScreens', () => {
    it('should return the list of lao navigation screens', () => {
      const { result } = renderHook(() => LaoHooks.useLaoNavigationScreens(), {
        wrapper: wrapper(mockStore),
      });

      // .toBe() checks whether the operands are the same object (at the same memory address)
      expect(result.current).toBe(laoNavigationScreens);
    });
  });

  describe('useOrganizerNavigationScreens', () => {
    it('should return the list of organizer navigation screens', () => {
      const { result } = renderHook(() => LaoHooks.useEventsNavigationScreens(), {
        wrapper: wrapper(mockStore),
      });

      // .toBe() checks whether the operands are the same object (at the same memory address)
      expect(result.current).toBe(organizerNavigationScreens);
    });
  });

  describe('useDisconnectFromLao', () => {
    it('should return the function to disconnect from a lao', () => {
      const { result } = renderHook(() => LaoHooks.useDisconnectFromLao(), {
        wrapper: wrapper(mockStore),
      });

      expect(result.current).toBeFunction();
    });
  });
});
