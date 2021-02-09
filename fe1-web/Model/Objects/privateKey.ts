import { Signature } from "./signature";
import { decodeBase64, encodeBase64 } from "tweetnacl-util";
import { sign } from "tweetnacl";
import { Base64Data } from "./base64";

export class PrivateKey extends Base64Data {

    /**
     * Sign some base64 data with the private key
     *
     * @param data the data to be signed with the private key
     */
    public sign(data: Base64Data): Signature {
        const signature = sign.detached(
            decodeBase64(data.toString()),
            decodeBase64(this.toString())
        );
        return new Signature(encodeBase64(signature));
    }
}
