import 'jest-extended';
import * as b64 from 'base-64';

declare global {
  namespace jest {
    // this interface is wrong, but necessary for TypeScript to be happy
    // received fields should be 'R', for accuracy
    // we put R in the last one should be instead, to prevent complaints from TypeScript

    interface Matchers<R> {
      toBeBase64(received: R): CustomMatcherResult;

      toBeJsonEqual(received: any, expected: R): CustomMatcherResult;

      toBeDistinctArray(received: R): jest.CustomMatcherResult;

      toBeBase64Array(received: R): jest.CustomMatcherResult;

      toBeKeySignatureArray(
        received: string, keyField: string, signature: R
      ): jest.CustomMatcherResult;
    }
  }
}

expect.extend({

  toBeJsonEqual(received: any, expected: any): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeJsonEqual matcher');
    }

    const r = JSON.parse(JSON.stringify(received));
    const ex = JSON.parse(JSON.stringify(expected));
    return {
      pass: this.equals(received, expected, undefined, false),
      message: () => `Value is not equal to expected value (under JSON transformation)\n\n${
        this.utils.printDiffOrStringify(
          ex,
          r,
          'Expected',
          'Received',
          true,
        )}`,
    };
  },

  toBeBase64(value: any): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeBase64 matcher');
    }

    try {
      b64.decode(value.toString());
      return {
        pass: true,
        message: () => `Expected '${value}' not to be a valid Base64 string`,
      };
    } catch (error) {
      return {
        pass: false,
        message: () => `Expected '${value}' to be a valid Base64 string`,
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

  toBeBase64Array(value: any): jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeBase64Array matcher');
    }

    expect(value).toBeArray();
    value.forEach((item: any) => {
      expect(item).toBeBase64();
    });

    return {
      pass: true,
      message: () => 'Expected value to be an array of base64 values',
    };
  },

  toBeKeySignatureArray(received: any, keyField: string, signature: string)
    : jest.CustomMatcherResult {
    if (this.isNot) {
      throw new Error('Unsupported negation on toBeKeySignatureArray matcher');
    }

    const expectedObj: any = {};
    expectedObj[keyField] = expect.toBeString();
    expectedObj[signature] = expect.toBeString();

    expect(received).toBeArray();
    received.forEach((item: any) => {
      expect(item).toBeObject();
      expect(item).toEqual(
        expect.objectContaining(expectedObj),
      );
      expect(item[keyField]).toBeBase64();
      expect(item[keyField].length).toEqual(44); /* EdDSA public key length in base64 */
      expect(item[signature]).toBeBase64();
    });
    return {
      pass: true,
      message: () => 'Expected value to be an array of key/signature values',
    };
  },
});
