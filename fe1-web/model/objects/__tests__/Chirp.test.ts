import 'jest-extended';
import '__tests__/utils/matchers';
import {
  Chirp, ChirpState,
} from '../Chirp';
import { ProtocolError } from '../../network';

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

  it('throws an error when object is undefined', () => {
    const createWrongChirp = () => {
      const wrongChirpState: ChirpState = {};
      Chirp.fromState(wrongChirpState);
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when id is undefined', () => {
    const createWrongChirp = () => {
      const wrongChirpState: ChirpState = {
        id: undefined,
        sender: 'me',
        text: 'text',
        time: 1234,
        likes: 0,
        parentId: '5678',
      };
      Chirp.fromState(wrongChirpState);
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when sender is undefined', () => {
    const createWrongChirp = () => {
      const wrongChirpState: ChirpState = {
        id: '1234',
        sender: undefined,
        text: 'text',
        time: 1234,
        likes: 0,
        parentId: '5678',
      };
      Chirp.fromState(wrongChirpState);
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when text is undefined', () => {
    const createWrongChirp = () => {
      const wrongChirpState: ChirpState = {
        id: '1234',
        sender: 'me',
        text: undefined,
        time: 1234,
        likes: 0,
        parentId: '5678',
      };
      Chirp.fromState(wrongChirpState);
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when time is undefined', () => {
    const createWrongChirp = () => {
      const wrongChirpState: ChirpState = {
        id: '1234',
        sender: 'me',
        text: 'text',
        time: undefined,
        likes: 0,
        parentId: '5678',
      };
      Chirp.fromState(wrongChirpState);
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error when likes is undefined', () => {
    const createWrongChirp = () => {
      const wrongChirpState: ChirpState = {
        id: '1234',
        sender: 'me',
        text: 'text',
        time: 1234,
        likes: undefined,
        parentId: '5678',
      };
      Chirp.fromState(wrongChirpState);
    };
    expect(createWrongChirp).toThrow(Error);
  });

  it('throws an error', () => {
    const throwError = () => {
      throw new ProtocolError('Error');
    };
    expect(throwError).toThrow('Error');
  });
});
