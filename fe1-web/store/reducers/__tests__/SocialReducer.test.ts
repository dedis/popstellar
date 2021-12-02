import 'jest-extended';
import { AnyAction } from 'redux';
import keyPair from 'test_data/keypair.json';
import { Hash, PublicKey, Timestamp } from 'model/objects';
import { Chirp } from 'model/objects/Chirp';
import { socialReduce, addChirp } from '../SocialReducer';

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

test('should return the initial state', () => {
  expect(socialReduce(undefined, {} as AnyAction))
    .toEqual(emptyState);
});

test('should add the first chirp correctly', () => {
  expect(socialReduce(emptyState, addChirp(mockLaoId, chirp1)))
    .toEqual(filledState1);
});

test('should add the newer chirp before the first chirp', () => {
  expect(socialReduce(filledState1, addChirp(mockLaoId, chirp2)))
    .toEqual(filledState2);
});

test('should add the newer chirp after the second chirp', () => {
  expect(socialReduce(filledState2, addChirp(mockLaoId, chirp3)))
    .toEqual(filledState3);
});

test('should add the newer chirp on top', () => {
  expect(socialReduce(filledState3, addChirp(mockLaoId, chirp4)))
    .toEqual(filledState4);
});
