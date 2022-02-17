import 'jest-extended';
import '__tests__/utils/matchers';
import { ProtocolError } from 'model/network';
import { ConnectToLao } from '../ConnectToLao';
import { Base64UrlData } from '../Base64Url';

const laoId = Base64UrlData.encode('lao_id').toString();
const serverUrl = 'ws://127.0.0.1:9000/organizer/client';

const sampleConnectToLao: Partial<ConnectToLao> = {
  server: serverUrl,
  lao: laoId,
};

const dataConnectToLao = `{
  "server": "${serverUrl}",
  "lao": "${laoId}"
  }`;

describe('ConnectToLao', () => {
  it('should be created correctly from JSON', () => {
    expect(new ConnectToLao(sampleConnectToLao)).toBeJsonEqual(sampleConnectToLao);
    const temp = {
      server: serverUrl,
      lao: laoId,
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
      lao: laoId,
    };
    const createFromJson = () => ConnectToLao.fromJson(obj);
    expect(createFromJson).toThrow(ProtocolError);
  });

  describe('constructor', () => {
    it('should throw an error if server is undefined', () => {
      const wrongObj = () => new ConnectToLao({
        lao: laoId,
      });
      expect(wrongObj).toThrow(ProtocolError);
    });

    it('should throw an error if lao id is undefined', () => {
      const wrongObj = () => new ConnectToLao({
        server: serverUrl,
      });
      expect(wrongObj).toThrow(ProtocolError);
    });
  });
});
