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

    const publicKey = new ElectionPublicKey(Base64UrlData.encode(aP.marshalBinary()));
    const privateKey = new ElectionPrivateKey(Base64UrlData.encode(a.marshalBinary()));
    return new ElectionKeyPair(privateKey, publicKey);
  }
}
