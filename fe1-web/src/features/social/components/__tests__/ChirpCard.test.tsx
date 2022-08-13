import { fireEvent, render } from '@testing-library/react-native';
import React from 'react';

import { mockLao, mockLaoIdHash, mockPopToken } from '__tests__/utils/TestUtils';
import FeatureContext from 'core/contexts/FeatureContext';
import { Hash, PublicKey, Timestamp } from 'core/objects';
import { OpenedLaoStore } from 'features/lao/store';
import STRINGS from 'resources/strings';

import { SOCIAL_FEATURE_IDENTIFIER } from '../../interface';
import {
  requestAddReaction as mockRequestAddReaction,
  requestDeleteChirp as mockRequestDeleteChirp,
} from '../../network/SocialMessageApi';
import { Chirp } from '../../objects';
import ChirpCard from '../ChirpCard';

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
    1234: {
      '👍': 1,
      '👎': 0,
      '❤️': 0,
    },
  })),
}));

jest.mock('core/components/ProfileIcon', () => () => 'ProfileIcon');

const contextValue = {
  [SOCIAL_FEATURE_IDENTIFIER]: {
    useCurrentLao: () => mockLao,
    getCurrentLao: () => mockLao,
    useCurrentLaoId: () => mockLaoIdHash,
    getCurrentLaoId: () => mockLaoIdHash,
    useRollCallById: () => undefined,
    useRollCallAttendeesById: () => [],
    generateToken: () => mockPopToken,
  },
};

// FIXME: useSelector mock doesn't seem to work correctly
describe('ChirpCard', () => {
  const renderChirp = (c: Chirp, publicKey: PublicKey) => {
    return render(
      <FeatureContext.Provider value={contextValue}>
        <ChirpCard chirp={c} currentUserPublicKey={publicKey} />
      </FeatureContext.Provider>,
    );
  };

  describe('for deletion', () => {
    const getMockLao = jest.spyOn(OpenedLaoStore, 'get');
    getMockLao.mockImplementation(() => mockLao);

    it('renders correctly for sender', () => {
      const obj = renderChirp(chirp, sender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly for non-sender', () => {
      const obj = renderChirp(chirp, new PublicKey('IAmNotTheSender'));
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('calls delete correctly', () => {
      const { getByLabelText, getByText } = renderChirp(chirp, sender);
      fireEvent.press(getByLabelText('deleteChirpButton'));
      fireEvent.press(getByText(STRINGS.general_yes));
      expect(mockRequestDeleteChirp).toHaveBeenCalledTimes(1);
    });

    it('render correct for a deleted chirp', () => {
      const obj = renderChirp(deletedChirp, sender);
      expect(obj.toJSON()).toMatchSnapshot();
    });
  });

  describe('for reaction', () => {
    it('renders correctly with reaction', () => {
      const obj = renderChirp(chirp, sender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('renders correctly without reaction', () => {
      const obj = renderChirp(chirp1, sender);
      expect(obj.toJSON()).toMatchSnapshot();
    });

    it('adds thumbs up correctly', () => {
      const { getByTestId } = renderChirp(chirp, sender);
      const thumbsUpButton = getByTestId('thumbs-up');
      fireEvent.press(thumbsUpButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👍', ID);
    });

    it('adds thumbs down correctly', () => {
      const { getByTestId } = renderChirp(chirp, sender);
      const thumbsDownButton = getByTestId('thumbs-down');
      fireEvent.press(thumbsDownButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('👎', ID);
    });

    it('adds heart correctly', () => {
      const { getByTestId } = renderChirp(chirp, sender);
      const heartButton = getByTestId('heart');
      fireEvent.press(heartButton);
      expect(mockRequestAddReaction).toHaveBeenCalledWith('❤️', ID);
    });
  });
});
