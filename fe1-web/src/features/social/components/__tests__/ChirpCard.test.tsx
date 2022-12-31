import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';

import MockNavigator from '__tests__/components/MockNavigator';
import { mockLao, mockLaoId, mockPopToken } from '__tests__/utils/TestUtils';
import FeatureContext from 'core/contexts/FeatureContext';
import { useActionSheet } from 'core/hooks/ActionSheet';
import { Hash, PublicKey, Timestamp } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';
import { SocialMediaContext } from 'features/social/context';

import { SocialReactContext, SOCIAL_FEATURE_IDENTIFIER } from '../../interface';
import { requestAddReaction as mockRequestAddReaction } from '../../network/SocialMessageApi';
import { Chirp } from '../../objects';
import ChirpCard from '../ChirpCard';

jest.mock('core/hooks/ActionSheet.ts', () => {
  const showActionSheet = jest.fn();
  return { useActionSheet: () => showActionSheet };
});

// eslint-disable-next-line react-hooks/rules-of-hooks
const showActionSheet = useActionSheet();

// region test data
const TIMESTAMP = 1609455600; // 31 December 2020
const sender = mockPopToken.publicKey;
const ID = new Hash('1234');

const chirp = new Chirp({
  id: ID,
  text: "Don't panic.",
  sender: sender,
  time: new Timestamp(TIMESTAMP),
  isDeleted: false,
});

const deletedChirp = new Chirp({
  id: new Hash('1234'),
  text: '',
  sender: sender,
  time: new Timestamp(TIMESTAMP),
  isDeleted: true,
});

const chirp1 = new Chirp({
  id: new Hash('5678'),
  text: 'Ignore me',
  sender: new PublicKey('Anonymous'),
  time: new Timestamp(TIMESTAMP),
});
// endregion

jest.mock('features/social/network/SocialMessageApi');
jest.mock('react-redux', () => ({
  ...jest.requireActual('react-redux'),
  useSelector: jest.fn(() => ({
    '👍': 1,
    '👎': 0,
    '❤️': 0,
  })),
}));

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

const senderContext = { currentUserPopTokenPublicKey: sender };
const nonSenderContext = { currentUserPopTokenPublicKey: new PublicKey('IAmNotTheSender') };

// FIXME: useSelector mock doesn't seem to work correctly
describe('ChirpCard', () => {
  const renderChirp = (c: Chirp, isSender: boolean) => {
    return render(
      <FeatureContext.Provider value={contextValue}>
        <SocialMediaContext.Provider value={isSender ? senderContext : nonSenderContext}>
          <MockNavigator
            component={() => <ChirpCard chirp={c} isFirstItem={false} isLastItem={false} />}
          />
        </SocialMediaContext.Provider>
      </FeatureContext.Provider>,
    );
  };

  describe('for deletion', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => mockLao);

    it('renders correctly for sender', () => {
      const obj = renderChirp(chirp, true);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly for non-sender', () => {
      const obj = renderChirp(chirp, false);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('options shown correctly', async () => {
      const { getByTestId } = renderChirp(chirp, true);
      fireEvent.press(getByTestId(`chirp_action_options`));

      expect(showActionSheet).toHaveBeenCalledTimes(1);
    });

    it('render correct for a deleted chirp', () => {
      const obj = renderChirp(deletedChirp, true);
      expect(obj.toJSON()).toMatchSnapshot();
    });
  });

  describe('for reaction', () => {
    it('renders correctly with reaction', () => {
      const obj = renderChirp(chirp, true);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly without reaction', () => {
      const obj = renderChirp(chirp1, true);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('adds thumbs up correctly', () => {
      const { getByTestId } = renderChirp(chirp, true);
      const thumbsUpButton = getByTestId('thumbs-up');
      fireEvent.press(thumbsUpButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👍', ID, mockLaoId);
    });

    it('adds thumbs down correctly', () => {
      const { getByTestId } = renderChirp(chirp, true);
      const thumbsDownButton = getByTestId('thumbs-down');
      fireEvent.press(thumbsDownButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👎', ID, mockLaoId);
    });

    it('adds heart correctly', () => {
      const { getByTestId } = renderChirp(chirp, true);
      const heartButton = getByTestId('heart');
      fireEvent.press(heartButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('❤️', ID, mockLaoId);
    });
  });
});
