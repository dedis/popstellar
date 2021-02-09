import { encodeBase64} from "tweetnacl-util";
import { PublicKey } from "./publicKey";
import { PrivateKey } from "./privateKey";
import { sign } from "tweetnacl";
import { getStore } from "../../Store/configureStore";

export class KeyPair {

    public static publicKey: PublicKey;
    public static privateKey: PrivateKey;

    /**
     * Initialize the keychain with a set of keys
     */
    public static initialise() {

        const { pubKey } = getStore().getState().keypairReducer;
        const { secKey } = getStore().getState().keypairReducer;

        if (pubKey.length === 0 || secKey.length === 0) {
            // generate a new keypair
            const pair = sign.keyPair();

            const keys = { pubKey: encodeBase64(pair.publicKey), secKey: encodeBase64(pair.secretKey) };
            this.publicKey = new PublicKey(keys.pubKey);
            this.privateKey = new PrivateKey(keys.secKey);

            getStore().dispatch({ type: 'SET_KEYPAIR', value: keys });

        } else {
            // fill with existing keys from storage
            this.publicKey = new PublicKey(pubKey);
            this.privateKey = new PrivateKey(secKey);
        }
    }
}
