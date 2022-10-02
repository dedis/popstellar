import { validateScannableMainPublicKey } from 'core/network/validation/Validator';
import { ProtocolError } from 'core/objects';
import { OmitMethods } from 'core/types';

/**
 * Object containing the server url and Lao id to generate the QR code of a Lao.
 */
export class ScannableIdentity {
  public readonly main_public_key: string;

  constructor(scannableIdentity: OmitMethods<ScannableIdentity>) {
    if (scannableIdentity === undefined || scannableIdentity === null) {
      throw new ProtocolError(
        'Error encountered while creating a ScannableIdentity object: undefined/null parameters',
      );
    }

    if (scannableIdentity.main_public_key === undefined) {
      throw new ProtocolError("undefined 'main_public_key' when creating 'ScannableIdentity'");
    }

    this.main_public_key = scannableIdentity.main_public_key;
  }

  public static fromJson(obj: any): ScannableIdentity {
    const { errors } = validateScannableMainPublicKey(obj);

    if (errors !== null) {
      throw new ProtocolError(`Invalid identity \n\n${errors}`);
    }

    return new ScannableIdentity({
      ...obj,
    });
  }

  public toJson(): string {
    return JSON.stringify({
      main_public_key: this.main_public_key,
    });
  }

  static encodeIdentity(params: OmitMethods<ScannableIdentity>): string {
    return new ScannableIdentity(params).toJson();
  }
}
