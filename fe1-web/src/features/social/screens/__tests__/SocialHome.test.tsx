import { fireEvent, render, waitFor } from '@testing-library/react-native';
import React from 'react';
import { Provider } from 'react-redux';
import { act } from 'react-test-renderer';
import { combineReducers, createStore } from 'redux';

import { mockKeyPair, mockLaoState } from '__tests__/utils';
import { laoReducer, setCurrentLao } from 'features/lao/reducer';
import { requestAddChirp } from 'features/social/network/SocialMessageApi';
import SocialReducer from 'features/social/reducer/SocialReducer';

import SocialHome from '../SocialHome';

jest.mock('features/social/network/SocialMessageApi', () => {
  const actual = jest.requireActual('features/social/network/SocialMessageApi');
  return {
    ...actual,
    requestAddChirp: jest.fn(() => Promise.resolve()),
  };
});

beforeEach(() => {
  jest.clearAllMocks();
});

const mockStore = createStore(combineReducers({ ...laoReducer, ...SocialReducer }));
mockStore.dispatch(setCurrentLao(mockLaoState));

describe('SocialHome', () => {
  it('renders correctly', () => {
    const { toJSON } = render(
      <Provider store={mockStore}>
        <SocialHome currentUserPublicKey={mockKeyPair.publicKey} />
      </Provider>,
    );
    expect(toJSON()).toMatchSnapshot();
  });

  it('is possible to publish chirps', async () => {
    const { getByTestId } = render(
      <Provider store={mockStore}>
        <SocialHome currentUserPublicKey={mockKeyPair.publicKey} />
      </Provider>,
    );

    const mockText = 'some chirp text';

    fireEvent.changeText(getByTestId('new_chirp_input'), mockText);
    fireEvent.press(getByTestId('new_chirp_publish'));

    await waitFor(() => {
      expect(requestAddChirp).toHaveBeenCalledWith(mockKeyPair.publicKey, mockText);
      expect(requestAddChirp).toHaveBeenCalledTimes(1);
    });
  });
});
