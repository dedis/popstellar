import * as b64 from 'base-64';

export class Base64Data extends String {

    public constructor(value: string) {
        super(value);

        // raise an exception if it's not base64 data
        b64.decode(value);
    }

    public static encode(text: string) : Base64Data {
        return new Base64Data(b64.encode(text));
    }

    public decode(): string {
        return b64.decode(this.valueOf());
    }

    public equals(o: Base64Data): boolean {
        return this.toString() === o.toString();
    }
}
