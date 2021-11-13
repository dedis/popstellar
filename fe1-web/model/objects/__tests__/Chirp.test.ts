import 'jest-extended';
import '__tests__/utils/matchers';
import {
  Chirp, ChirpState,
} from '../Chirp';
import { Timestamp } from '../Timestamp';
import { Hash } from '../Hash';

const TIMESTAMP = new Timestamp(12345);
const HASH_ID = new Hash('id');
const HASH_PARENT = new Hash('parent');

describe('Chirp object', () => {
  it('can do a state round trip', () => {
    const chirpState: ChirpState = {
      id: '1234',
      sender: 'me',
      text: 'text',
      time: 1234,
      likes: 0,
      parentId: '5678',
    };
    const chirp = Chirp.fromState(chirpState);
    expect(chirp.toState()).toStrictEqual(chirpState);
  });

  it('throws an error when id is undefined', () => {
    const createWrongChirp = () => {
      Chirp.constructor({
        sender: 'me',
        text: 'text',
        time: TIMESTAMP,
        likes: 0,
        parentId: HASH_PARENT,
      });
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when sender is undefined', () => {
    const createWrongChirp = () => {
      Chirp.constructor({
        id: HASH_ID,
        text: 'text',
        time: TIMESTAMP,
        likes: 0,
        parentId: HASH_PARENT,
      });
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when text is undefined', () => {
    const createWrongChirp = () => {
      Chirp.constructor({
        id: HASH_ID,
        sender: 'me',
        time: TIMESTAMP,
        likes: 0,
        parentId: HASH_PARENT,
      });
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when time is undefined', () => {
    const createWrongChirp = () => {
      Chirp.constructor({
        id: HASH_ID,
        sender: 'me',
        text: 'text',
        likes: 0,
        parentId: HASH_PARENT,
      });
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when likes is undefined', () => {
    const createWrongChirp = () => {
      Chirp.constructor({
        id: HASH_ID,
        sender: 'me',
        text: 'text',
        time: TIMESTAMP,
        parentId: HASH_PARENT,
      });
    };
    expect(createWrongChirp).toThrow(Error);
  });
});
