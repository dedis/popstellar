import { sha256 } from 'js-sha256';
import { Base64Data } from "./base64";

export class Hash extends Base64Data {

    /**
     * Create a base64 encoded hash of an array of strings according to the communication protocol
     *
     * @param data values to be hashed
     * @return resulting hash
     */
    public static fromStringArray(...data: string[]): Hash {
        let str = '';
        data.forEach((item) => { str += `"${Hash._escapeString(item)}",`; });
        // remove the last comma and add square brackets around
        str = `[${str.slice(0, -1)}]`;

        return Base64Data.encode(this.fromString(str));
    }

    /**
     * Create a hash of a string
     *
     * @param data value to be hashed
     * @return resulting hash
     */
    public static fromString(data: string): string {
        const hash = sha256.create();

        const bString = hash.update(data).array();
        return String.fromCharCode(...bString);
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
