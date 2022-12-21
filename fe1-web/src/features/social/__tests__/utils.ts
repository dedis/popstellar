import { Hash, PublicKey, Timestamp } from 'core/objects';

import { Chirp, Reaction } from '../objects';

export const mockSender1 = new PublicKey('Douglas Adams');
export const mockSender2 = new PublicKey('Gandalf');
export const mockChirpId0 = new Hash('000');
export const mockChirpId1 = new Hash('1234');
export const mockChirpId2 = new Hash('5678');
export const mockChirpId3 = new Hash('12345');
export const mockChirpId4 = new Hash('123456');
export const mockChirpTimestamp = new Timestamp(1606666600);

export const mockChirp0 = new Chirp({
  id: mockChirpId0,
  sender: mockSender1,
  text: "Don't delete me!",
  time: mockChirpTimestamp,
  isDeleted: false,
});

export const mockChirp1 = new Chirp({
  id: mockChirpId1,
  sender: mockSender1,
  text: "Don't panic.",
  time: new Timestamp(1605555500),
  isDeleted: false,
});

export const mockChirp2 = new Chirp({
  id: mockChirpId2,
  sender: mockSender2,
  text: 'You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass! You shall not pass!',
  time: new Timestamp(1607777700),
});

export const mockChirp3 = new Chirp({
  id: mockChirpId3,
  sender: mockSender1,
  text: 'Time is an illusion',
  time: mockChirpTimestamp,
});

export const mockChirp4 = new Chirp({
  id: mockChirpId4,
  sender: mockSender1,
  text: 'The answer is 42',
  time: new Timestamp(1608888800),
});

export const mockChirp0DeletedFake = new Chirp({
  id: mockChirpId0,
  sender: new PublicKey('Joker'),
  text: '',
  time: mockChirpTimestamp,
  isDeleted: true,
});

export const mockChirp1Deleted = new Chirp({
  id: mockChirpId1,
  sender: mockSender1,
  text: '',
  time: new Timestamp(1605555500),
  isDeleted: true,
});

export const mockChirp1DeletedFake = new Chirp({
  id: mockChirpId1,
  sender: mockSender2,
  text: '',
  time: new Timestamp(1605555500),
  isDeleted: true,
});

export const mockChirp4Deleted = new Chirp({
  id: mockChirpId4,
  sender: mockSender1,
  text: '',
  time: new Timestamp(1608888800),
  isDeleted: true,
});

export const mockReaction1 = new Reaction({
  id: new Hash('1111'),
  sender: mockSender1,
  codepoint: '👍',
  chirpId: mockChirpId1,
  time: mockChirpTimestamp,
});

export const mockReaction2 = new Reaction({
  id: new Hash('2222'),
  sender: mockSender1,
  codepoint: '❤️',
  chirpId: mockChirpId1,
  time: mockChirpTimestamp,
});

export const mockReaction3 = new Reaction({
  id: new Hash('3333'),
  sender: mockSender2,
  codepoint: '👍',
  chirpId: mockChirpId1,
  time: mockChirpTimestamp,
});

export const mockReaction4 = new Reaction({
  id: new Hash('4444'),
  sender: mockSender2,
  codepoint: '👍',
  chirpId: mockChirpId2,
  time: mockChirpTimestamp,
});
