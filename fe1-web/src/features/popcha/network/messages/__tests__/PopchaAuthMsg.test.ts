import { validateDataObject } from 'core/network/validation';
import { Hash, ProtocolError } from 'core/objects';

import { PopchaAuthMsg } from '../PopchaAuthMsg';

const CLIENT_ID = 'client-id';
const NONCE = 'nonce';
const POPCHA_ADDRESS = 'popcha-address';
const IDENTIFIER = new Hash('mock-identifier');
const IDENTIFIER_PROOF = new Hash('mock-identifier-proof');
const STATE = 'state';
const RESPONSE_MODE = 'response-mode';

const VALID_POPCHA_AUTH_MSG = {
  client_id: CLIENT_ID,
  nonce: NONCE,
  popcha_address: POPCHA_ADDRESS,
  identifier: IDENTIFIER,
  identifier_proof: IDENTIFIER_PROOF,
  state: STATE,
  response_mode: RESPONSE_MODE,
};

jest.mock('core/network/validation', () => ({
  validateDataObject: jest.fn(),
}));

describe('PopchaAuthMsg', () => {
  describe('constructor', () => {
    it('should throw an error if client_id is undefined', () => {
      expect(
        () =>
          new PopchaAuthMsg({
            nonce: NONCE,
            popcha_address: POPCHA_ADDRESS,
            identifier: IDENTIFIER,
            identifier_proof: IDENTIFIER_PROOF,
          }),
      ).toThrow(ProtocolError);
    });

    it('should throw an error if nonce is undefined', () => {
      expect(
        () =>
          new PopchaAuthMsg({
            client_id: CLIENT_ID,
            popcha_address: POPCHA_ADDRESS,
            identifier: IDENTIFIER,
            identifier_proof: IDENTIFIER_PROOF,
          }),
      ).toThrow(ProtocolError);
    });

    it('should throw an error if identifier is undefined', () => {
      expect(
        () =>
          new PopchaAuthMsg({
            client_id: CLIENT_ID,
            nonce: NONCE,
            popcha_address: POPCHA_ADDRESS,
            identifier_proof: IDENTIFIER_PROOF,
          }),
      ).toThrow(ProtocolError);
    });

    it('should throw an error if identifier_proof is undefined', () => {
      expect(
        () =>
          new PopchaAuthMsg({
            client_id: CLIENT_ID,
            nonce: NONCE,
            popcha_address: POPCHA_ADDRESS,
            identifier: IDENTIFIER,
          }),
      ).toThrow(ProtocolError);
    });

    it('should throw an error if popcha_address is undefined', () => {
      expect(
        () =>
          new PopchaAuthMsg({
            client_id: CLIENT_ID,
            nonce: NONCE,
            identifier: IDENTIFIER,
            identifier_proof: IDENTIFIER_PROOF,
          }),
      ).toThrow(ProtocolError);
    });

    it('should set state if provided', () => {
      const msg = new PopchaAuthMsg({
        client_id: CLIENT_ID,
        nonce: NONCE,
        identifier: IDENTIFIER,
        identifier_proof: IDENTIFIER_PROOF,
        popcha_address: POPCHA_ADDRESS,
        state: STATE,
      });
      expect(msg.state).toEqual(STATE);
    });

    it('should set response_mode if provided', () => {
      const msg = new PopchaAuthMsg({
        client_id: CLIENT_ID,
        nonce: NONCE,
        identifier: IDENTIFIER,
        identifier_proof: IDENTIFIER_PROOF,
        popcha_address: POPCHA_ADDRESS,
        response_mode: RESPONSE_MODE,
      });
      expect(msg.response_mode).toEqual(RESPONSE_MODE);
    });

    it('should not throw an error if all required parameters are provided', () => {
      expect(
        () =>
          new PopchaAuthMsg({
            client_id: CLIENT_ID,
            nonce: NONCE,
            identifier: IDENTIFIER,
            identifier_proof: IDENTIFIER_PROOF,
            popcha_address: POPCHA_ADDRESS,
          }),
      ).not.toThrow(ProtocolError);
    });
  });

  describe('from JSON', () => {
    it('invalid JSON should throw an error', () => {
      (validateDataObject as jest.Mock).mockReturnValue({ errors: 'error' });
      expect(() => PopchaAuthMsg.fromJson('invalid')).toThrow(ProtocolError);
    });
    it('valid JSON should return a PopchaAuthMsg', () => {
      (validateDataObject as jest.Mock).mockReturnValue({ errors: null });
      expect(PopchaAuthMsg.fromJson(VALID_POPCHA_AUTH_MSG)).toBeInstanceOf(PopchaAuthMsg);
    });
  });
});
