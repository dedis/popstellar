import 'jest-extended';
import { AnyAction } from 'redux';
import keyPair from 'test_data/keypair.json';
import { Hash, PublicKey, Timestamp } from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import {
  socialReduce, addChirp, makeChirpsList, makeChirpsListOfUser,
} from '../SocialReducer';

const mockPublicKey = new PublicKey(keyPair.publicKey);
const org = mockPublicKey;
const name = 'MyLao';
const mockLaoIdHash: Hash = Hash.fromStringArray(
  org.toString(), new Timestamp(160000000).toString(), name,
);
const mockLaoId: string = mockLaoIdHash.toString();

const chirp1 = new Chirp({
  id: Hash.fromString('1234'),
  sender: new PublicKey('Douglas Adams'),
  text: 'Don\'t panic.',
  time: new Timestamp(1605555500),
  likes: 100,
}).toState();

const chirp2 = new Chirp({
  id: Hash.fromString('5678'),
  sender: new PublicKey('Gandalf'),
  text: 'You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass!',
  time: new Timestamp(1607777700),
  likes: 0,
}).toState();

const chirp3 = new Chirp({
  id: Hash.fromString('12345'),
  sender: new PublicKey('Douglas Adams'),
  text: 'Time is an illusion',
  time: new Timestamp(1606666600),
  likes: 98,
}).toState();

const chirp4 = new Chirp({
  id: Hash.fromString('123456'),
  sender: new PublicKey('Douglas Adams'),
  text: 'The answer is 42',
  time: new Timestamp(1608888800),
  likes: 42,
}).toState();

const emptyState = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
    },
  },
};

const filledState1 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp1.id],
      byId: { [chirp1.id]: chirp1 },
      byUser: { [chirp1.sender]: [chirp1.id] },
    },
  },
};

const filledState2 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp2.id, chirp1.id],
      byId: { [chirp1.id]: chirp1, [chirp2.id]: chirp2 },
      byUser: { [chirp1.sender]: [chirp1.id], [chirp2.sender]: [chirp2.id] },
    },
  },
};

const filledState3 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp2.id, chirp3.id, chirp1.id],
      byId: { [chirp1.id]: chirp1, [chirp2.id]: chirp2, [chirp3.id]: chirp3 },
      byUser: { [chirp1.sender]: [chirp3.id, chirp1.id], [chirp2.sender]: [chirp2.id] },
    },
  },
};

const filledState4 = {
  byLaoId: {
    myLaoId: {
      allIdsInOrder: [],
      byId: {},
      byUser: {},
    },
    [mockLaoId]: {
      allIdsInOrder: [chirp4.id, chirp2.id, chirp3.id, chirp1.id],
      byId: {
        [chirp1.id]: chirp1,
        [chirp2.id]: chirp2,
        [chirp3.id]: chirp3,
        [chirp4.id]: chirp4,
      },
      byUser: { [chirp1.sender]: [chirp4.id, chirp3.id, chirp1.id], [chirp2.sender]: [chirp2.id] },
    },
  },
};

describe('SocialReducer', () => {
  describe('reducer', () => {
    it('should return the initial state', () => {
      expect(socialReduce(undefined, {} as AnyAction))
        .toEqual(emptyState);
    });

    it('should add the first chirp correctly', () => {
      expect(socialReduce(emptyState, addChirp(mockLaoId, chirp1)))
        .toEqual(filledState1);
    });

    it('should add the newer chirp before the first chirp', () => {
      expect(socialReduce(filledState1, addChirp(mockLaoId, chirp2)))
        .toEqual(filledState2);
    });

    it('should add the newer chirp after the second chirp', () => {
      expect(socialReduce(filledState2, addChirp(mockLaoId, chirp3)))
        .toEqual(filledState3);
    });

    it('should add the newest chirp on top', () => {
      expect(socialReduce(filledState3, addChirp(mockLaoId, chirp4)))
        .toEqual(filledState4);
    });
  });

  describe('selector', () => {
    it('should return an empty list of chirpState when no lao is opened', () => {
      expect(makeChirpsList().resultFunc(emptyState, undefined))
        .toEqual([]);
    });

    it('should return an empty list', () => {
      socialReduce(undefined, {} as AnyAction);
      expect(makeChirpsList().resultFunc(emptyState, mockLaoId))
        .toEqual([]);
    });

    it('should return the first chirp state', () => {
      socialReduce(emptyState, addChirp(mockLaoId, chirp1));
      expect(makeChirpsList().resultFunc(filledState1, mockLaoId))
        .toEqual([chirp1]);
    });

    it('should return the newer chirp before the first chirp', () => {
      socialReduce(filledState1, addChirp(mockLaoId, chirp2));
      expect(makeChirpsList().resultFunc(filledState2, mockLaoId))
        .toEqual([chirp2, chirp1]);
    });

    it('should add the newer chirp after the second chirp', () => {
      socialReduce(filledState2, addChirp(mockLaoId, chirp3));
      expect(makeChirpsList().resultFunc(filledState3, mockLaoId))
        .toEqual([chirp2, chirp3, chirp1]);
    });

    it('should return the newest chirp on top', () => {
      socialReduce(filledState3, addChirp(mockLaoId, chirp4));
      expect(makeChirpsList().resultFunc(filledState4, mockLaoId))
        .toEqual([chirp4, chirp2, chirp3, chirp1]);
    });

    it('should return the correct chirps list for an active user', () => {
      expect(makeChirpsListOfUser(chirp1.sender).resultFunc(filledState3, mockLaoId))
        .toEqual([chirp3, chirp1]);
    });

    it('should return an empty list for an inactive user', () => {
      expect(makeChirpsListOfUser(chirp2.sender).resultFunc(filledState1, mockLaoId))
        .toEqual([]);
    });

    it('should return an empty list for an undefined lao', () => {
      expect(makeChirpsListOfUser(chirp2.sender).resultFunc(filledState1, undefined))
        .toEqual([]);
    });
  });
});
