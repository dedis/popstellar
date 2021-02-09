import * as b64 from 'base-64';

export class Base64Data extends String {

    private constructor(value: string) {
        super(value);
    }

    public static encode(text: string) : Base64Data {
        return new Base64Data(b64.encode(text));
    }

    public decode(): string {
        return b64.decode(this.valueOf());
    }
}