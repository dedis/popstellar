import 'jest-extended';
import '__tests__/utils/matchers';
import {
  Chirp, ChirpState,
} from '../Chirp';
import { Timestamp } from '../Timestamp';
import { Hash } from '../Hash';
import { PublicKey } from '../PublicKey';

const TIMESTAMP = new Timestamp(12345);
const HASH_ID = new Hash('id');
const HASH_PARENT = new Hash('parent');
const PK = new PublicKey('publicKey');
const TEXT = 'text';

describe('Chirp object', () => {
  it('does a state round trip correctly', () => {
    const chirpState: ChirpState = {
      id: '1234',
      sender: 'me',
      text: TEXT,
      time: 1234,
      likes: 6,
      parentId: '5678',
      isDeleted: false,
    };
    const chirp = Chirp.fromState(chirpState);
    expect(chirp.toState()).toStrictEqual(chirpState);
  });

  describe('constructor', () => {
    it('throws an error when object is undefined', () => {
      const partial = undefined as unknown as Partial<Chirp>;
      const createWrongChirp = () => new Chirp(partial);
      expect(createWrongChirp).toThrow(Error);
    });

    it('throws an error when object is null', () => {
      const partial = null as unknown as Partial<Chirp>;
      const createWrongChirp = () => new Chirp(partial);
      expect(createWrongChirp).toThrow(Error);
    });

    it('throws an error when id is undefined', () => {
      const createWrongChirp = () => new Chirp({
        sender: PK,
        text: TEXT,
        time: TIMESTAMP,
        likes: 0,
        parentId: HASH_PARENT,
      });
      expect(createWrongChirp).toThrow(Error);
    });

    it('throws an error when sender is undefined', () => {
      const createWrongChirp = () => new Chirp({
        id: HASH_ID,
        text: TEXT,
        time: TIMESTAMP,
        likes: 0,
        parentId: HASH_PARENT,
      });
      expect(createWrongChirp).toThrow(Error);
    });

    it('throws an error when text is undefined', () => {
      const createWrongChirp = () => new Chirp({
        id: HASH_ID,
        sender: PK,
        time: TIMESTAMP,
        likes: 0,
        parentId: HASH_PARENT,
      });
      expect(createWrongChirp).toThrow(Error);
    });

    it('throws an error when time is undefined', () => {
      const createWrongChirp = () => new Chirp({
        id: HASH_ID,
        sender: PK,
        text: TEXT,
        likes: 0,
        parentId: HASH_PARENT,
      });
      expect(createWrongChirp).toThrow(Error);
    });

    it('initializes likes to zero if it is undefined', () => {
      const newChirp = new Chirp({
        id: HASH_ID,
        sender: PK,
        text: TEXT,
        time: TIMESTAMP,
        parentId: HASH_PARENT,
      });
      expect(newChirp.likes).toStrictEqual(0);
    });
  });
});
