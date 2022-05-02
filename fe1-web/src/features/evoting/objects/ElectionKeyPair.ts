import { curve } from '@dedis/kyber';

import { Base64UrlData } from 'core/objects';

import { ElectionPrivateKey } from './ElectionPrivateKey';
import { ElectionPublicKey } from './ElectionPublicKey';

const ed25519 = curve.newCurve('edwards25519');

export class ElectionKeyPair {
  public privateKey: ElectionPrivateKey;

  public publicKey: ElectionPublicKey;

  constructor(privateKey: ElectionPrivateKey, publicKey: ElectionPublicKey) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  static generate() {
    const a = ed25519.scalar().pick();
    const aP = ed25519.point().base().mul(a);

    const publicKey = new ElectionPublicKey(aP.toProto().toString('base64'));
    const privateKey = new ElectionPrivateKey(a.marshalBinary().toString('base64'));
    return new ElectionKeyPair(privateKey, publicKey);
  }
}
