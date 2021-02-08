import { PublicKey } from './publicKey';

export class Signature extends String {

    public verify(key: PublicKey, data: string): boolean {
        return false;
    }
}