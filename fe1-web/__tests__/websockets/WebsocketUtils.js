/* eslint-disable */

import { sign } from 'tweetnacl';
import { decodeBase64, decodeUTF8, encodeBase64, encodeUTF8 } from 'tweetnacl-util';
const wsUtils = require('../../websockets/WebsocketUtils');


const assert = require('assert');
const assertChai = require('chai').assert;



describe('=== WebsocketUtils tests ===', function() {

  describe('websockets.WebsocketUtils.fromString64:toString64', function () {
    const str = 'string to encode/decode';
    const str64 = 'c3RyaW5nIHRvIGVuY29kZS9kZWNvZGU=';

    it('should decode/encode properly', function () {
      assertChai.strictEqual(wsUtils.fromString64(str64), str);
      assertChai.strictEqual(wsUtils.toString64(str), str64);
    });

    it('should be inverse functions', function () {
      assertChai.strictEqual(wsUtils.fromString64(wsUtils.toString64(str)), str);
    });

    it('should comply with tweetnacl decode/encode functions', function () {
      assertChai.strictEqual(wsUtils.toString64(str), encodeBase64(decodeUTF8(str)));
      assertChai.strictEqual(wsUtils.fromString64(str64), encodeUTF8(decodeBase64(str64)));
    });
  });


  describe('websockets.WebsocketUtils.escapeString', function () {

    it('should not change an empty string', function () {
      const str = '';
      assertChai.strictEqual(wsUtils.escapeString(str), str);
    });

    it('should not change a simple string', function () {
      const str = 'salutations';
      assertChai.strictEqual(wsUtils.escapeString(str), str);
    });

    it('should escape a double-quote \'"\'', function () {
      const str = '"';
      assertChai.strictEqual(wsUtils.escapeString(str), '\\"');
    });

    it('should escape a backlash \'\\\'', function () {
      const str = '\\';
      assertChai.strictEqual(wsUtils.escapeString(str), '\\\\');
    });

    it('should not escape random characters', function () {
      const str = 'qwertzuiopasdfghjklyxcvbnmèél,.-àè1234567890œ∑€∂ƒ√∫~~°';
      assertChai.strictEqual(wsUtils.escapeString(str), str);
    });

    it('should escape a simple json example', function () {
      const str = '{"type":"rollcall"}';
      assertChai.strictEqual(wsUtils.escapeString(str), '{\\\"type\\\":\\\"rollcall\\\"}');
    });

    it('should escape a string with multiple \'"\' and \'\\\'', function () {
      let str = '"jsonrpc": 2.0, "path":"C:Users\\Wexus\\Document"';
      assertChai.strictEqual(wsUtils.escapeString(str), '\\"jsonrpc\\": 2.0, \\"path\\":\\"C:Users\\\\Wexus\\\\Document\\"');

      str = '\\""""\\\\\\"\\\\\\\\';
      assertChai.strictEqual(wsUtils.escapeString(str), '\\\\\\"\\"\\"\\"\\\\\\\\\\\\\\"\\\\\\\\\\\\\\\\');
    });

    it('should escape be1 example correctly', function () {
      const str = '{"object":"lao","action":"create","id":"JZFuR7yIBcLedUReDE+OuyIKZn+QpyGq2PZcFYWZQDQ=","name":"oui oui","creation":1609777492,"organizer":"mK0eAXHPPlxySr1erjOhZNlKz34/+nJ1hi1Sph66fas=","witnesses":[]}';
      const res = '{\\"object\\":\\"lao\\",\\"action\\":\\"create\\",\\"id\\":\\"JZFuR7yIBcLedUReDE+OuyIKZn+QpyGq2PZcFYWZQDQ=\\",\\"name\\":\\"oui oui\\",\\"creation\\":1609777492,\\"organizer\\":\\"mK0eAXHPPlxySr1erjOhZNlKz34/+nJ1hi1Sph66fas=\\",\\"witnesses\\":[]}';
      assertChai.strictEqual(wsUtils.escapeString(str), res);
    });
  });


  describe('websockets.WebsocketUtils.signStrings', function () {

    const _generateNewKeyPair = () => {
      const pair = sign.keyPair();
      return { pubKey: encodeBase64(pair.publicKey), secKey: encodeBase64(pair.secretKey) };
    };

    it('should sign an empty string', function () {
      const str = '';
      const keyPair = _generateNewKeyPair();
      const signature = wsUtils.signString(str, keyPair.secKey);
      assertChai.isTrue(sign.detached.verify(decodeUTF8(str), decodeBase64(signature), decodeBase64(keyPair.pubKey)));
    });

    it('should sign a single string', function () {
      const str = 'message to be signed';
      const keyPair = _generateNewKeyPair();
      const signature = wsUtils.signString(str, keyPair.secKey);
      assertChai.isTrue(sign.detached.verify(decodeUTF8(str), decodeBase64(signature), decodeBase64(keyPair.pubKey)));
    });
  });


  describe('websockets.WebsocketUtils.hashStrings', function () {

    // https://base64.guru/converter/decode/hex
    function base64ToHex(str) {
      const raw = atob(str);
      let result = '';
      for (let i = 0; i < raw.length; i++) {
        const hex = raw.charCodeAt(i).toString(16);
        result += (hex.length === 2 ? hex : '0' + hex);
      }
      return result.toLowerCase();
    }


    it('should hash an empty string', function () {
      const str = '';
      const hexExpected = '055539df4a0b804c58caf46c0cd2941af10d64c1395ddd8e50b5f55d945841e6';
      assertChai.strictEqual(base64ToHex(wsUtils.hashStrings(str)), hexExpected);
    });

    it('should hash a single string', function () {
      const str = 'message to be hashed';
      const hexExpected = '929d03e777354a0cdb260fcc8a5da618936a3de20e3732b9be14efbf793fc7c0';
      assertChai.strictEqual(base64ToHex(wsUtils.hashStrings(str)), hexExpected);
    });

    it('should hash a single escaped string', function () {
      let str = 'this is a double quote: "';
      let hexExpected = 'd8e9c8422ba98529253bdc9a23fb2275ed44b1b1a758c57f74324379291a1a0f';
      assertChai.strictEqual(base64ToHex(wsUtils.hashStrings(str)), hexExpected);

      str = 'this is a backslash: \\';
      hexExpected = 'dc28c990f9eb99ac74559e4aaa3d54c806d37fc503f85f13e5ea089809c48d8d';
      assertChai.strictEqual(base64ToHex(wsUtils.hashStrings(str)), hexExpected);

      str = '"this is a string with backslash: \\" and there\'s a backslash: \\. Thats it';
      hexExpected = 'fea4659afafae3d0ccf833d535d2ab6331df9e39563449fa4affa25d0cc0fcf0';
      assertChai.strictEqual(base64ToHex(wsUtils.hashStrings(str)), hexExpected);
    });

    it('should hash multiple strings', function () {
      const str1 = '{"type":"rollcall"}';
      const str2 = 'c2lnbmF0dXJl';
      const hexExpected = 'd8a4fc4cd72c82df967c932c3182c976772f2f517e84c0659e73d2ca2776bcdb';
      assertChai.strictEqual(base64ToHex(wsUtils.hashStrings(str1, str2)), hexExpected);
    });
  });
});
