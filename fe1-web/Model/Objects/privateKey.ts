import { Signature } from "./signature";

export class PrivateKey extends String {

    /**
     * Sign some data with the private key
     *
     * @param data the data to be signed with the private key
     */
    public sign(data: string) : Signature {
        return new Signature('to be implemented');
    }
}