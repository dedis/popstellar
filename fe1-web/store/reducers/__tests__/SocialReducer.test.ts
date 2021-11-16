import 'jest-extended';
import { AnyAction } from 'redux';
import { ChirpState } from 'model/objects/Chirp';
import { reducer, addChirp } from '../SocialReducer';
import { OpenedLaoStore } from 'store/stores';

const laoId = OpenedLaoStore.get().id;

const cs: ChirpState = {
  id: '1234',
  sender: 'me',
  text: 'text',
  time: 1234,
  likes: 6,
  parentId: '5678',
};

const emptySocialState = {
  byLaoId: {
    myLaoId: {
      allChirps: [],
    },
  },
}

const socialState = {
  byLaoId: {
    myLaoId: {
      allChirps: [cs],
    },
  },
}

describe('SocialReducer', () => {
  it('should return the initial state', () => {
    expect(reducer(undefined, {} as AnyAction)).toEqual(emptySocialState);
  });

  it('should add a chirp correctly', () => {
    expect(reducer(undefined, addChirp(laoId, cs))).toEqual(socialState);
  });
});
