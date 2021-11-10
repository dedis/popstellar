import 'jest-extended';
import { Chirp, ChirpState } from '../Chirp';

describe('Chirp object', () => {
  it('can do a state round-trip', () => {
    const chirpState: ChirpState = {
      sender: 'me',
      message: 'Chirp',
      time: 1234,
      likes: 28,
      dislikes: 3,
      replies: 2,
    };
    const chirp: Chirp = Chirp.fromState(chirpState);
    expect(chirp.toState()).toStrictEqual(chirpState);
  });
});
