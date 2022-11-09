import { validateScannablePopToken } from 'core/network/validation/Validator';
import { ProtocolError } from 'core/objects';
import { OmitMethods } from 'core/types';

/**
 * Object containing the server url and Lao id to generate the QR code of a Lao.
 */
export class ScannablePopToken {
  public readonly pop_token: string;

  constructor(scannedPopToken: OmitMethods<ScannablePopToken>) {
    if (scannedPopToken === undefined || scannedPopToken === null) {
      throw new ProtocolError(
        'Error encountered while creating a ScannedPopToken object: undefined/null parameters',
      );
    }

    if (scannedPopToken.pop_token === undefined) {
      throw new ProtocolError("undefined 'pop_token' when creating 'ScannedPopToken'");
    }

    this.pop_token = scannedPopToken.pop_token;
  }

  public static fromJson(obj: any): ScannablePopToken {
    const { errors } = validateScannablePopToken(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid pop token\n\n${errors}`);
    }

    return new ScannablePopToken({
      ...obj,
    });
  }

  public toJson(): string {
    return JSON.stringify({
      pop_token: this.pop_token,
    });
  }

  static encodePopToken(params: OmitMethods<ScannablePopToken>): string {
    return new ScannablePopToken(params).toJson();
  }
}
