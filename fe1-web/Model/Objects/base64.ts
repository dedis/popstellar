export class Base64Data extends String {

    private constructor(value: string) {
        super(value);
    }

    public static encode(text: string) : Base64Data {
        return new Base64Data(btoa(text));
    }

    public decode(): string {
        return atob(this.valueOf());
    }
}