import { combineReducers, configureStore } from '@reduxjs/toolkit';
import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { Store } from 'redux';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils/TestUtils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash, PublicKey } from 'core/objects';
import { setCurrentLao, laoReducer } from 'features/lao/reducer';
import { OpenedLaoStore } from 'features/lao/store';
import { mockChirp0, mockChirp0Deleted, mockChirpTimestamp } from 'features/social/__tests__/utils';
import { SocialMediaContext } from 'features/social/context';
import { addChirp, addReaction, socialReducer } from 'features/social/reducer';
import STRINGS from 'resources/strings';

import { SocialReactContext, SOCIAL_FEATURE_IDENTIFIER } from '../../interface';
import {
  requestAddReaction as mockRequestAddReaction,
  requestDeleteReaction as mockRequestDeleteReaction,
} from '../../network/SocialMessageApi';
import { Chirp, Reaction } from '../../objects';
import ChirpCard from '../ChirpCard';

jest.mock('core/hooks/ActionSheet.ts', () => {
  const showActionSheet = jest.fn();
  return { useActionSheet: () => showActionSheet };
});

jest.mock('features/social/network/SocialMessageApi');
jest.mock('core/components/ProfileIcon', () => () => 'ProfileIcon');

const contextValue = {
  [SOCIAL_FEATURE_IDENTIFIER]: {
    useCurrentLao: () => mockLao,
    getCurrentLao: () => mockLao,
    useConnectedToLao: () => true,
    useCurrentLaoId: () => mockLaoId,
    getCurrentLaoId: () => mockLaoId,
    useRollCallById: () => undefined,
    useRollCallAttendeesById: () => [],
    generateToken: () => Promise.resolve(mockPopToken),
  } as SocialReactContext,
};

const { sender } = mockChirp0;
const nonSender = new PublicKey('IAmNotTheSender');
const reactionSender = new PublicKey('IAmTheOneReacting');

const thumbsUpReaction = new Reaction({
  id: new Hash('1111'),
  sender: reactionSender,
  codepoint: '👍',
  chirpId: mockChirp0.id,
  time: mockChirpTimestamp,
});

const thumbsDownReaction = new Reaction({
  id: new Hash('2222'),
  sender: reactionSender,
  codepoint: '👎',
  chirpId: mockChirp0.id,
  time: mockChirpTimestamp,
});

const getMockStore = () => {
  const mockStore = configureStore({
    reducer: combineReducers({ ...socialReducer, ...laoReducer }),
  });
  mockStore.dispatch(setCurrentLao(mockLao));

  return mockStore;
};

beforeEach(() => {
  jest.clearAllMocks();
});

// FIXME: useSelector mock doesn't seem to work correctly
describe('ChirpCard', () => {
  const renderChirp = (mockStore: Store, c: Chirp, user: PublicKey) => {
    return render(
      // render chirp using <Provider> to make ChirpCard use mockStore
      <Provider store={mockStore}>
        {/* each feature requires its feature context due to how the dependency injection is set up */}
        <FeatureContext.Provider value={contextValue}>
          <SocialMediaContext.Provider value={{ currentUserPopTokenPublicKey: user }}>
            {/* components using 'useNavigation' or 'useRoute' need a MockNavigator wrapped around them */}
            <MockNavigator
              component={() => <ChirpCard chirp={c} isFirstItem={false} isLastItem={false} />}
            />
          </SocialMediaContext.Provider>
        </FeatureContext.Provider>
      </Provider>,
    );
  };

  describe('for deletion', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => mockLao);

    it('renders correctly for sender', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));

      const obj = renderChirp(mockStore, mockChirp0, sender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly for non-sender', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));

      const obj = renderChirp(mockStore, mockChirp0, nonSender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('render correct for a deleted chirp', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0Deleted));

      const obj = renderChirp(mockStore, mockChirp0Deleted, nonSender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('delete shows confirmation windows', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0Deleted));

      const { getByText, getByTestId } = renderChirp(mockStore, mockChirp0, sender);
      fireEvent.press(getByTestId('delete_chirp'));

      expect(getByText(STRINGS.social_media_ask_confirm_delete_chirp)).toBeTruthy();
    });
  });

  describe('for reaction', () => {
    it('renders correctly with reaction', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));
      mockStore.dispatch(addReaction(mockLaoId, thumbsUpReaction));

      const obj = renderChirp(mockStore, mockChirp0, nonSender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly without reaction', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));

      const obj = renderChirp(mockStore, mockChirp0, nonSender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('removes thumbs up correctly', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));
      mockStore.dispatch(addReaction(mockLaoId, thumbsUpReaction));

      const { getByTestId } = renderChirp(mockStore, mockChirp0, reactionSender);
      const thumbsUpButton = getByTestId('thumbs-up');
      fireEvent.press(thumbsUpButton);
      expect(mockRequestDeleteReaction).toHaveBeenCalledWith(thumbsUpReaction.id, mockLaoId);
    });

    it('adds thumbs down correctly', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));

      const { getByTestId } = renderChirp(mockStore, mockChirp0, reactionSender);
      const thumbsDownButton = getByTestId('thumbs-down');
      fireEvent.press(thumbsDownButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👎', mockChirp0.id, mockLaoId);
    });

    it('adds heart correctly', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));

      const { getByTestId } = renderChirp(mockStore, mockChirp0, reactionSender);
      const heartButton = getByTestId('heart');
      fireEvent.press(heartButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('❤️', mockChirp0.id, mockLaoId);
    });

    it('removes thumbs up when reacting with thumbs down', () => {
      const mockStore = getMockStore();
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));
      mockStore.dispatch(addReaction(mockLaoId, thumbsUpReaction));

      const { getByTestId } = renderChirp(mockStore, mockChirp0, reactionSender);
      const thumbsDownButton = getByTestId('thumbs-down');

      fireEvent.press(thumbsDownButton);
      expect(mockRequestDeleteReaction).toHaveBeenCalledWith(thumbsUpReaction.id, mockLaoId);
    });

    it('removes thumbs down when reacting with thumbs up', () => {
      // prepare the redux store with a chirp where 'sender' has previously reacted
      // with a thumbs up
      const mockStore = getMockStore();
      // add a chirp to the redux store
      mockStore.dispatch(addChirp(mockLaoId, mockChirp0));
      // add the thumbs down reaction to the redux store
      mockStore.dispatch(addReaction(mockLaoId, thumbsDownReaction));

      const { getByTestId } = renderChirp(mockStore, mockChirp0, reactionSender);
      const thumbsUpButton = getByTestId('thumbs-up');
      fireEvent.press(thumbsUpButton);

      expect(mockRequestDeleteReaction).toHaveBeenCalledWith(thumbsDownReaction.id, mockLaoId);
      expect(mockRequestAddReaction).toHaveBeenCalledTimes(1);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👍', mockChirp0.id, mockLaoId);
    });
  });
});
