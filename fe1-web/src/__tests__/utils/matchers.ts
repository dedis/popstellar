import 'jest-extended';
import base64url from 'base64url';

declare global {
  namespace jest {
    // this interface is wrong, but necessary for TypeScript to be happy
    // received fields should be 'R', for accuracy
    // we put R in the last argument instead, to prevent complaints from TypeScript

    interface Matchers<R> {
      toBeBase64Url(received: R): CustomMatcherResult;

      toBeJsonEqual(received: unknown, expected: R): CustomMatcherResult;

      toBeDistinctArray(received: R): jest.CustomMatcherResult;

      toBeBase64UrlArray(received: R): jest.CustomMatcherResult;

      toBeKeySignatureArray(
        received: string,
        keyField: string,
        signature: R,
      ): jest.CustomMatcherResult;

      toBeNumberObject(received: R): jest.CustomMatcherResult;
    }
  }
}

expect.extend({
  toBeNumberObject(received: unknown): jest.CustomMatcherResult {
    return {
      pass: typeof received === 'number' || received instanceof Number,
      message: () => `Expected '${received}' to be a number or number object`,
    };
  },

  toBeJsonEqual(received: unknown, expected: unknown): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeJsonEqual matcher');
    }

    const r = JSON.parse(JSON.stringify(received));
    const ex = JSON.parse(JSON.stringify(expected));

    expect(r).toEqual(ex);
    return {
      pass: true,
      message: () => 'Value is equal to expected value (under JSON transformation)',
    };
  },

  toBeBase64Url(value: any): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeBase64Url matcher');
    }

    try {
      base64url.decode(value.toString());
      return {
        pass: true,
        message: () => `Expected '${value}' not to be a valid Base64Url string`,
      };
    } catch (error) {
      return {
        pass: false,
        message: () => `Expected '${value}' to be a valid Base64Url string`,
      };
    }
  },

  toBeDistinctArray(receiver: any): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeDistinctArray matcher');
    }

    expect(receiver).toBeArray();

    return {
      pass: receiver.length === new Set(receiver).size,
      message: () => 'Expected value to have only distinct values',
    };
  },

  toBeBase64UrlArray(value: unknown): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeBase64Array matcher');
    }

    expect(value).toBeArray();
    (value as unknown[]).forEach((item: unknown) => {
      expect(item).toBeBase64Url();
    });

    return {
      pass: true,
      message: () => 'Expected value to be an array of base64 values',
    };
  },

  toBeKeySignatureArray(
    received: any,
    keyField: string,
    signature: string,
  ): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeKeySignatureArray matcher');
    }

    const expectedObj: any = {};
    expectedObj[keyField] = expect.toBeString();
    expectedObj[signature] = expect.toBeString();

    expect(received).toBeArray();
    received.forEach((item: any) => {
      expect(item).toBeObject();
      expect(item).toEqual(expect.objectContaining(expectedObj));
      expect(item[keyField]).toBeBase64Url();
      expect(item[keyField].length).toEqual(44); /* EdDSA public key length in base64 */
      expect(item[signature]).toBeBase64Url();
    });
    return {
      pass: true,
      message: () => 'Expected value to be an array of key/signature values',
    };
  },
});
