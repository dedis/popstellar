import { PublicKey } from './PublicKey';
import { sign } from "tweetnacl";
import { decodeBase64 } from "tweetnacl-util";
import { Base64Data } from "./Base64";

export class Signature extends Base64Data {

    /**
     * Verify the signature for the message data and return true iff verification succeeded
     *
     * @param key public key of the presumed sender
     * @param data base64 signed message
     * @return true iff the signature verification succeeded
     */
    public verify(key: PublicKey, data: Base64Data): boolean {
        return sign.detached.verify(
            decodeBase64(data.toString()),
            decodeBase64(this.toString()),
            decodeBase64(key.toString())
        );
    }
}
