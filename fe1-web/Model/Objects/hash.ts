import { sha256 } from 'js-sha256';
import { Base64Data } from "./base64";

export class Hash extends Base64Data {

    /**
     * Create a base64 encoded hash of data according to the communication protocol
     *
     * @param data value to be hashed
     * @return resulting hash
     */
    public static fromString(...data: string[]): Hash {
        let str = '';
        data.forEach((item) => { str += `"${Hash._escapeString(item)}",`; });
        // remove the last comma and add square brackets around
        str = `[${str.slice(0, -1)}]`;

        const hash = sha256.create();

        const bString = hash.update(str).array();
        return Base64Data.encode(String.fromCharCode(...bString));
    }

    /**
     * Escapes backslashes and double-quotes in a string
     *
     * @param str string to be escaped
     * @private
     */
    private static _escapeString(str: string): string {
        return str.replace(/\\/g, '\\\\').replace(/"/g, '\\"');
    }
}
