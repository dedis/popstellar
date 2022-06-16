import 'jest-extended';
import '__tests__/utils/matchers';

import { mockLaoId } from '__tests__/utils/TestUtils';
import { ProtocolError } from 'core/objects';

import { ConnectToLao } from '../ConnectToLao';

const serverUrl = 'ws://127.0.0.1:9000/organizer/client';

const sampleConnectToLao: Partial<ConnectToLao> = {
  servers: [serverUrl],
  lao: mockLaoId,
};

const dataConnectToLao = `{
  "server": "${serverUrl}",
  "lao": "${mockLaoId}"
  }`;

describe('ConnectToLao', () => {
  it('should be created correctly from JSON', () => {
    expect(new ConnectToLao(sampleConnectToLao)).toBeJsonEqual(sampleConnectToLao);
    const temp = {
      servers: [serverUrl],
      lao: mockLaoId,
    };
    expect(new ConnectToLao(temp)).toBeJsonEqual(temp);
  });

  it('should be parsed correctly from JSON', () => {
    const obj = JSON.parse(dataConnectToLao);
    expect(ConnectToLao.fromJson(obj)).toBeJsonEqual(sampleConnectToLao);
  });

  it('fromJSON should throw an error if the Json has invalid server url', () => {
    const obj = {
      server: '12345678',
      lao: mockLaoId,
    };
    const createFromJson = () => ConnectToLao.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if server is undefined', () => {
      const wrongObj = () =>
        new ConnectToLao({
          lao: mockLaoId,
        });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao id is undefined', () => {
      const wrongObj = () =>
        new ConnectToLao({
          servers: [serverUrl],
        });
      expect(wrongObj).toThrow(ProtocolError);
    });
  });
});
